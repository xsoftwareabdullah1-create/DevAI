#include "tokenizer.h"
#include <fstream>
#include <map>
#include <cstdint>
#include <limits>

namespace {
using Symbols = std::vector<std::string>;
constexpr char kMagic[] = "DVBPE01\n";
constexpr std::uint32_t kMaxMerges = 4096;
constexpr std::uint32_t kMaxSymbolBytes = 1024 * 1024;

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
bool writeU32(std::ostream& out, std::uint32_t value) {
    char bytes[4] = {static_cast<char>(value), static_cast<char>(value >> 8),
                     static_cast<char>(value >> 16), static_cast<char>(value >> 24)};
    out.write(bytes, 4);
    return static_cast<bool>(out);
}
bool readU32(std::istream& in, std::uint32_t& value) {
    unsigned char b[4];
    if (!in.read(reinterpret_cast<char*>(b), 4)) return false;
    value = static_cast<std::uint32_t>(b[0]) |
            (static_cast<std::uint32_t>(b[1]) << 8) |
            (static_cast<std::uint32_t>(b[2]) << 16) |
            (static_cast<std::uint32_t>(b[3]) << 24);
    return true;
}
bool writeString(std::ostream& out, const std::string& value) {
    if (value.empty() || value.size() > kMaxSymbolBytes ||
        !writeU32(out, static_cast<std::uint32_t>(value.size()))) return false;
    out.write(value.data(), static_cast<std::streamsize>(value.size()));
    return static_cast<bool>(out);
}
bool readString(std::istream& in, std::string& value) {
    std::uint32_t size = 0;
    if (!readU32(in, size) || size == 0 || size > kMaxSymbolBytes) return false;
    value.resize(size);
    return static_cast<bool>(in.read(&value[0], static_cast<std::streamsize>(size)));
}
}

bool ByteBpeTokenizer::train(const std::string& corpus, int mergeCount) {
    merges_.clear();
    vocabulary_.clear();
    trained_ = false;
    if (corpus.empty() || mergeCount < 0 || mergeCount > static_cast<int>(kMaxMerges)) return false;
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

bool ByteBpeTokenizer::save(const std::string& path) const {
    if (!trained_ || path.empty() || merges_.size() > kMaxMerges) return false;
    std::ofstream out(path, std::ios::binary | std::ios::trunc);
    if (!out) return false;
    out.write(kMagic, sizeof(kMagic) - 1);
    if (!writeU32(out, static_cast<std::uint32_t>(merges_.size()))) return false;
    for (const auto& merge : merges_)
        if (!writeString(out, merge.first) || !writeString(out, merge.second)) return false;
    return static_cast<bool>(out);
}

bool ByteBpeTokenizer::load(const std::string& path) {
    std::ifstream in(path, std::ios::binary);
    if (!in) return false;
    char magic[sizeof(kMagic) - 1];
    if (!in.read(magic, sizeof(magic)) ||
        std::string(magic, sizeof(magic)) != std::string(kMagic, sizeof(kMagic) - 1)) return false;
    std::uint32_t count = 0;
    if (!readU32(in, count) || count > kMaxMerges) return false;

    std::vector<std::pair<std::string, std::string>> loadedMerges;
    Symbols loadedVocabulary;
    loadedVocabulary.reserve(256 + count);
    for (int b = 0; b < 256; ++b)
        loadedVocabulary.emplace_back(1, static_cast<char>(b));
    for (std::uint32_t i = 0; i < count; ++i) {
        std::string left, right;
        if (!readString(in, left) || !readString(in, right)) return false;
        bool leftKnown = false, rightKnown = false;
        for (const auto& token : loadedVocabulary) {
            leftKnown = leftKnown || token == left;
            rightKnown = rightKnown || token == right;
        }
        if (!leftKnown || !rightKnown || left.size() > kMaxSymbolBytes - right.size()) return false;
        loadedMerges.emplace_back(left, right);
        loadedVocabulary.push_back(left + right);
    }
    if (in.peek() != std::char_traits<char>::eof()) return false;
    merges_.swap(loadedMerges);
    vocabulary_.swap(loadedVocabulary);
    trained_ = true;
    return true;
}
