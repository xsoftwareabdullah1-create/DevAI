#include "../app/src/main/cpp/transformer.h"
#include <cassert>
#include <cmath>
#include <iostream>
#include <vector>
static bool close(float a,float b,float tol=1e-5f){return std::fabs(a-b)<=tol;}
int main(){
 TransformerConfig c;c.vocabSize=300;c.dimension=16;c.heads=4;c.layers=1;c.feedForwardDimension=32;c.maxSequenceLength=16;c.seed=42;
 TinyTransformer model(c);assert(model.valid());const std::vector<int> ids{4,9,12,3};const auto logits=model.forward(ids);
 assert(logits.size()==ids.size()*static_cast<std::size_t>(c.vocabSize));for(float x:logits)assert(std::isfinite(x));
 const auto longer=model.forward(std::vector<int>{4,9,12,3,17});assert(longer.size()==5u*c.vocabSize);
 for(std::size_t i=0;i<4u*c.vocabSize;++i)assert(close(logits[i],longer[i],2e-5f));
 TinyTransformer same(c);assert(same.forward(ids)==logits);c.seed=43;TinyTransformer other(c);assert(other.forward(ids)!=logits);
 assert(model.forward({}).empty());assert(model.forward({-1}).empty());assert(model.forward({300}).empty());
 assert(model.forward(std::vector<int>(17,1)).empty());c.dimension=15;c.heads=4;TinyTransformer invalid(c);assert(!invalid.valid());
 c.dimension=32;c.heads=4;c.vocabSize=65536;c.layers=32;c.feedForwardDimension=8192;TinyTransformer oversized(c);assert(!oversized.valid());
 std::cout<<"DevAI Transformer tests passed: finite logits, causal masking, seed reproducibility, and bounds.\n";
}
