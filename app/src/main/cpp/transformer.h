#pragma once
#include <cstdint>
#include <vector>
struct TransformerConfig {
 int vocabSize=320, dimension=32, heads=4, layers=1, feedForwardDimension=64, maxSequenceLength=64;
 std::uint32_t seed=1337;
};
class TinyTransformer {
public:
 explicit TinyTransformer(const TransformerConfig& config);
 bool valid() const { return valid_; }
 std::vector<float> forward(const std::vector<int>& tokenIds) const;
private:
 struct Layer {
  std::vector<float> attentionNorm, feedForwardNorm;
  std::vector<float> query,key,value,attentionOutput,gate,up,down;
 };
 TransformerConfig config_; bool valid_=false;
 std::vector<float> tokenEmbedding_,finalNorm_,languageHead_;
 std::vector<Layer> layers_;
 void initializeMatrix(std::vector<float>& matrix,int rows,int columns,std::uint32_t& state);
};
