#pragma once
#include <string>
#include <utility>
#include <vector>

// Educational byte-level BPE: byte IDs are 0..255; learned merges start at 256.
class ByteBpeTokenizer {
public:
    bool train(const std::string& corpus, int mergeCount);
    std::vector<int> encode(const std::string& text) const;
    std::string decode(const std::vector<int>& ids) const;
    int vocabularySize() const;
    bool isTrained() const { return trained_; }

    // Versioned binary vocabulary format. Returns false on I/O or validation errors.
    bool save(const std::string& path) const;
    bool load(const std::string& path);

private:
    std::vector<std::pair<std::string, std::string>> merges_;
    std::vector<std::string> vocabulary_;
    bool trained_ = false;
};
