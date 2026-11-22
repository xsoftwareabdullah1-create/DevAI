#include "tokenizer.h"
#include <map>

namespace {
using Symbols = std::vector<std::string>;
Symbols splitBytes(const std::string& text) {
    Symbols out;
    out.reserve(text.size());
    for (unsigned char b : text) out.emplace_back(1, static_cast<char>(b));
    return out;
}
void mergePair(Symbols& symbols, const std::string& left, const std::string& right) {
    Symbols out;
    out.reserve(symbols.size());
    for (std::size_t i = 0; i < symbols.size();) {
        if (i + 1 < symbols.size() && symbols[i] == left && symbols[i + 1] == right) {
            out.push_back(left + right);
            i += 2;
        } else {
            out.push_back(symbols[i++]);
        }
    }
    symbols.swap(out);
}
}

bool ByteBpeTokenizer::train(const std::string& corpus, int mergeCount) {
    merges_.clear();
    vocabulary_.clear();
    trained_ = false;
    if (corpus.empty() || mergeCount < 0) return false;
    Symbols symbols = splitBytes(corpus);
    for (int step = 0; step < mergeCount; ++step) {
        std::map<std::pair<std::string, std::string>, int> counts;
        for (std::size_t i = 0; i + 1 < symbols.size(); ++i)
            ++counts[{symbols[i], symbols[i + 1]}];
        if (counts.empty()) break;
        auto best = counts.begin();
        for (auto it = counts.begin(); it != counts.end(); ++it)
            if (it->second > best->second) best = it;
        if (best->second < 2) break;
        const auto pair = best->first;
        merges_.push_back(pair);
        mergePair(symbols, pair.first, pair.second);
    }
    for (int b = 0; b < 256; ++b)
        vocabulary_.emplace_back(1, static_cast<char>(b));
    for (const auto& p : merges_) vocabulary_.push_back(p.first + p.second);
    trained_ = true;
    return true;
}

std::vector<int> ByteBpeTokenizer::encode(const std::string& text) const {
    if (!trained_) return {};
    Symbols symbols = splitBytes(text);
    for (const auto& p : merges_) mergePair(symbols, p.first, p.second);
    std::vector<int> ids;
    ids.reserve(symbols.size());
    for (const auto& symbol : symbols) {
        int id = -1;
        for (std::size_t i = 0; i < vocabulary_.size(); ++i)
            if (vocabulary_[i] == symbol) { id = static_cast<int>(i); break; }
        if (id < 0) return {};
        ids.push_back(id);
    }
    return ids;
}

std::string ByteBpeTokenizer::decode(const std::vector<int>& ids) const {
    if (!trained_) return {};
    std::string out;
    for (int id : ids) {
        if (id < 0 || static_cast<std::size_t>(id) >= vocabulary_.size()) return {};
        out += vocabulary_[static_cast<std::size_t>(id)];
    }
    return out;
}
int ByteBpeTokenizer::vocabularySize() const {
    return trained_ ? static_cast<int>(vocabulary_.size()) : 0;
}
