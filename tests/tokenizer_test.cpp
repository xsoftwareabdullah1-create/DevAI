#include "../app/src/main/cpp/tokenizer.h"

#include <cassert>
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

    assert(tokenizer.decode(std::vector<int>{-1}).empty());
    assert(tokenizer.decode(std::vector<int>{tokenizer.vocabularySize()}).empty());

    std::cout << "DevAI tokenizer tests passed. Vocabulary: "
              << tokenizer.vocabularySize() << "\n";
    return 0;
}
