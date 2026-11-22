#include "transformer.h"
#include <algorithm>
#include <cmath>
#include <cstdint>
#include <limits>
namespace {
std::uint32_t nextRandom(std::uint32_t& s){s^=s<<13;s^=s>>17;s^=s<<5;return s;}
float randomSigned(std::uint32_t& s){return static_cast<float>(nextRandom(s)&0x00FFFFFFu)/8388607.5f-1.0f;}
void rmsNorm(const float* x,const std::vector<float>& w,float* y,int d){
 double sum=0;for(int i=0;i<d;++i)sum+=static_cast<double>(x[i])*x[i];
 const float inv=1.0f/std::sqrt(static_cast<float>(sum/d)+1.0e-5f);
 for(int i=0;i<d;++i)y[i]=x[i]*inv*w[static_cast<std::size_t>(i)];
}
void matVec(const std::vector<float>& w,const float* x,float* y,int rows,int cols){
 for(int r=0;r<rows;++r){double sum=0;const std::size_t off=static_cast<std::size_t>(r)*cols;
  for(int c=0;c<cols;++c)sum+=static_cast<double>(w[off+c])*x[c];y[r]=static_cast<float>(sum);}
}
float silu(float x){if(x>20.0f)return x;if(x< -20.0f)return 0.0f;return x/(1.0f+std::exp(-x));}
void rope(float* x,int hd,int pos){
 for(int i=0;i+1<hd;i+=2){const float freq=std::pow(10000.0f,-static_cast<float>(i)/hd);
  const float angle=pos*freq,cs=std::cos(angle),sn=std::sin(angle),a=x[i],b=x[i+1];
  x[i]=a*cs-b*sn;x[i+1]=a*sn+b*cs;}
}
}
TinyTransformer::TinyTransformer(const TransformerConfig& c):config_(c){
 if(c.vocabSize<2||c.vocabSize>65536||c.dimension<2||c.dimension>2048||c.heads<1||c.heads>64||
    c.layers<1||c.layers>32||c.feedForwardDimension<1||c.feedForwardDimension>8192||
    c.maxSequenceLength<1||c.maxSequenceLength>4096||c.dimension%c.heads!=0||
    (c.dimension/c.heads)%2!=0)return;
 const std::uint64_t d=c.dimension,v=c.vocabSize,f=c.feedForwardDimension,l=c.layers;
 const std::uint64_t params=2*v*d+l*(4*d*d+3*d*f+2*d);
 if(params>2000000u)return;
 std::uint32_t state=c.seed==0?0xA341316Cu:c.seed;
 tokenEmbedding_.resize(static_cast<std::size_t>(c.vocabSize)*c.dimension);
 initializeMatrix(tokenEmbedding_,c.vocabSize,c.dimension,state);
 layers_.resize(static_cast<std::size_t>(c.layers));
 for(auto& z:layers_){z.attentionNorm.assign(c.dimension,1.0f);z.feedForwardNorm.assign(c.dimension,1.0f);
  initializeMatrix(z.query,c.dimension,c.dimension,state);initializeMatrix(z.key,c.dimension,c.dimension,state);
  initializeMatrix(z.value,c.dimension,c.dimension,state);initializeMatrix(z.attentionOutput,c.dimension,c.dimension,state);
  initializeMatrix(z.gate,c.feedForwardDimension,c.dimension,state);initializeMatrix(z.up,c.feedForwardDimension,c.dimension,state);
  initializeMatrix(z.down,c.dimension,c.feedForwardDimension,state);}
 finalNorm_.assign(c.dimension,1.0f);initializeMatrix(languageHead_,c.vocabSize,c.dimension,state);valid_=true;
}
void TinyTransformer::initializeMatrix(std::vector<float>& w,int rows,int cols,std::uint32_t& s){
 w.resize(static_cast<std::size_t>(rows)*cols);const float scale=std::sqrt(2.0f/static_cast<float>(rows+cols));
 for(float& x:w)x=randomSigned(s)*scale;
}
std::vector<float> TinyTransformer::forward(const std::vector<int>& ids)const{
 if(!valid_||ids.empty()||ids.size()>static_cast<std::size_t>(config_.maxSequenceLength))return {};
 for(int id:ids)if(id<0||id>=config_.vocabSize)return {};
 const int seq=static_cast<int>(ids.size()),d=config_.dimension,heads=config_.heads,hd=d/heads,ff=config_.feedForwardDimension;
 const std::size_t n=static_cast<std::size_t>(seq)*d;
 std::vector<float>x(n),norm(n),q(n),k(n),v(n),attn(n),proj(n),ffIn(d),gate(ff),up(ff),ffOut(d),scores(seq);
 for(int t=0;t<seq;++t)std::copy_n(tokenEmbedding_.data()+static_cast<std::size_t>(ids[t])*d,d,x.data()+static_cast<std::size_t>(t)*d);
 for(const Layer& z:layers_){
  for(int t=0;t<seq;++t){const std::size_t o=static_cast<std::size_t>(t)*d;rmsNorm(x.data()+o,z.attentionNorm,norm.data()+o,d);
   matVec(z.query,norm.data()+o,q.data()+o,d,d);matVec(z.key,norm.data()+o,k.data()+o,d,d);matVec(z.value,norm.data()+o,v.data()+o,d,d);
   for(int h=0;h<heads;++h){rope(q.data()+o+h*hd,hd,t);rope(k.data()+o+h*hd,hd,t);}}
  std::fill(attn.begin(),attn.end(),0.0f);const float scale=1.0f/std::sqrt(static_cast<float>(hd));
  for(int t=0;t<seq;++t)for(int h=0;h<heads;++h){const float* query=q.data()+static_cast<std::size_t>(t)*d+h*hd;
   float maxScore=-std::numeric_limits<float>::infinity();
   for(int j=0;j<=t;++j){const float* key=k.data()+static_cast<std::size_t>(j)*d+h*hd;double dot=0;
    for(int c=0;c<hd;++c)dot+=static_cast<double>(query[c])*key[c];scores[j]=static_cast<float>(dot)*scale;maxScore=std::max(maxScore,scores[j]);}
   double denom=0;for(int j=0;j<=t;++j){scores[j]=std::exp(scores[j]-maxScore);denom+=scores[j];}
   float* out=attn.data()+static_cast<std::size_t>(t)*d+h*hd;
   for(int j=0;j<=t;++j){const float p=static_cast<float>(scores[j]/denom);const float* value=v.data()+static_cast<std::size_t>(j)*d+h*hd;
    for(int c=0;c<hd;++c)out[c]+=p*value[c];}}
  for(int t=0;t<seq;++t){const std::size_t o=static_cast<std::size_t>(t)*d;matVec(z.attentionOutput,attn.data()+o,proj.data()+o,d,d);
   for(int c=0;c<d;++c)x[o+c]+=proj[o+c];rmsNorm(x.data()+o,z.feedForwardNorm,ffIn.data(),d);
   matVec(z.gate,ffIn.data(),gate.data(),ff,d);matVec(z.up,ffIn.data(),up.data(),ff,d);
   for(int c=0;c<ff;++c)gate[c]=silu(gate[c])*up[c];matVec(z.down,gate.data(),ffOut.data(),d,ff);
   for(int c=0;c<d;++c)x[o+c]+=ffOut[c];}
 }
 std::vector<float> logits(static_cast<std::size_t>(seq)*config_.vocabSize),finalState(d);
 for(int t=0;t<seq;++t){const std::size_t o=static_cast<std::size_t>(t)*d;rmsNorm(x.data()+o,finalNorm_,finalState.data(),d);
  matVec(languageHead_,finalState.data(),logits.data()+static_cast<std::size_t>(t)*config_.vocabSize,config_.vocabSize,d);}
 return logits;
}
