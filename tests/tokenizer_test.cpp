#include "../app/src/main/cpp/tokenizer.h"

#include <cassert>
#include <cstdio>
#include <fstream>
#include <iostream>
#include <string>
#include <vector>

int main() {
    ByteBpeTokenizer tokenizer;
    assert(!tokenizer.isTrained());
    assert(tokenizer.encode("hello").empty());
    assert(tokenizer.train("hello hello hello\n", 32));
    assert(tokenizer.isTrained());
    assert(tokenizer.vocabularySize() >= 256);

    const std::string sample = "hello hello";
    const std::vector<int> ids = tokenizer.encode(sample);
    assert(!ids.empty());
    assert(tokenizer.decode(ids) == sample);
    assert(tokenizer.decode(tokenizer.encode("")) == "");

    ByteBpeTokenizer second;
    assert(second.train("hello hello hello\n", 32));
    assert(second.encode(sample) == ids);

    const std::string path = "/tmp/devai-tokenizer-test.bpe";
    assert(tokenizer.save(path));
    ByteBpeTokenizer restored;
    assert(restored.load(path));
    assert(restored.vocabularySize() == tokenizer.vocabularySize());
    assert(restored.encode(sample) == ids);
    assert(restored.decode(ids) == sample);
    std::remove(path.c_str());

    const std::string malformed = "/tmp/devai-tokenizer-malformed.bpe";
    { std::ofstream out(malformed, std::ios::binary); out << "not-a-model"; }
    ByteBpeTokenizer rejected;
    assert(!rejected.load(malformed));
    assert(!rejected.isTrained());
    std::remove(malformed.c_str());

    assert(tokenizer.decode(std::vector<int>{-1}).empty());
    assert(tokenizer.decode(std::vector<int>{tokenizer.vocabularySize()}).empty());

    std::cout << "DevAI tokenizer tests passed. Vocabulary: "
              << tokenizer.vocabularySize() << "\n";
    return 0;
}
