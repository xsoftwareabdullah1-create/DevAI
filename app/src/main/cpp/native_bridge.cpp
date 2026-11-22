#include <jni.h>
#include <algorithm>
#include <cstddef>
#include <mutex>
#include <sstream>
#include <string>
#include <vector>
#include "tokenizer.h"
#include "transformer.h"

namespace {
ByteBpeTokenizer gTokenizer;
std::mutex gMutex;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_devai_MainActivity_nativeTokenizerInfo(JNIEnv* env, jobject) {
    std::lock_guard<std::mutex> lock(gMutex);
    std::ostringstream out;
    out << "Dev Orus 1.1 - DevAI original model\nDevAI byte-level BPE tokenizer\nStatus: "
        << (gTokenizer.isTrained() ? "trained from supplied text" : "not trained")
        << "\nVocabulary: " << gTokenizer.vocabularySize()
        << " tokens\nBase IDs 0-255 are bytes; learned merges start at 256."
        << "\nTokenizer only - it does not train model weights.";
    return env->NewStringUTF(out.str().c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_devai_MainActivity_nativeTrainTokenizer(JNIEnv* env, jobject, jstring corpus, jint mergeCount) {
    if (!corpus) return env->NewStringUTF("Training text is empty.");
    const char* chars = env->GetStringUTFChars(corpus, nullptr);
    if (!chars) return env->NewStringUTF("Could not read training text.");
    std::string text(chars);
    env->ReleaseStringUTFChars(corpus, chars);
    std::lock_guard<std::mutex> lock(gMutex);
    if (!gTokenizer.train(text, static_cast<int>(mergeCount)))
        return env->NewStringUTF("Training failed. Enter non-empty text.");
    std::ostringstream out;
    out << "Tokenizer training complete.\nVocabulary: " << gTokenizer.vocabularySize()
        << "\nLearned merges: " << gTokenizer.vocabularySize() - 256
        << "\nThis does not train neural-network weights.";
    return env->NewStringUTF(out.str().c_str());
}

extern "C" JNIEXPORT jintArray JNICALL
Java_com_devai_MainActivity_nativeEncode(JNIEnv* env, jobject, jstring input) {
    if (!input) return env->NewIntArray(0);
    const char* chars = env->GetStringUTFChars(input, nullptr);
    if (!chars) return env->NewIntArray(0);
    std::string text(chars);
    env->ReleaseStringUTFChars(input, chars);
    std::lock_guard<std::mutex> lock(gMutex);
    const std::vector<int> ids = gTokenizer.encode(text);
    jintArray result = env->NewIntArray(static_cast<jsize>(ids.size()));
    if (result && !ids.empty()) {
        std::vector<jint> values(ids.begin(), ids.end());
        env->SetIntArrayRegion(result, 0, static_cast<jsize>(values.size()), values.data());
    }
    return result;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_devai_MainActivity_nativeDecode(JNIEnv* env, jobject, jintArray input) {
    if (!input) return env->NewStringUTF("");
    const jsize length = env->GetArrayLength(input);
    std::vector<jint> values(static_cast<std::size_t>(length));
    if (length > 0) env->GetIntArrayRegion(input, 0, length, values.data());
    std::vector<int> ids(values.begin(), values.end());
    std::lock_guard<std::mutex> lock(gMutex);
    const std::string decoded = gTokenizer.decode(ids);
    return env->NewStringUTF(decoded.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_devai_MainActivity_nativeRunTransformer(JNIEnv* env, jobject, jintArray input) {
    if (!input) return env->NewStringUTF("No token IDs supplied. Encode text first.");
    const jsize length = env->GetArrayLength(input);
    if (length <= 0 || length > 64)
        return env->NewStringUTF("Tiny Transformer accepts 1-64 token IDs.");
    std::vector<jint> raw(static_cast<std::size_t>(length));
    env->GetIntArrayRegion(input, 0, length, raw.data());
    std::vector<int> ids(raw.begin(), raw.end());

    TransformerConfig config;
    config.vocabSize = 512;
    config.dimension = 32;
    config.heads = 4;
    config.layers = 1;
    config.feedForwardDimension = 64;
    config.maxSequenceLength = 64;
    config.seed = 1337;

    TinyTransformer model(config);
    if (!model.valid()) return env->NewStringUTF("Could not initialize tiny Transformer.");
    const std::vector<float> logits = model.forward(ids);
    if (logits.empty()) return env->NewStringUTF("Forward pass rejected the input IDs.");

    const std::size_t offset = (ids.size() - 1) * static_cast<std::size_t>(config.vocabSize);
    const auto first = logits.begin() + static_cast<std::ptrdiff_t>(offset);
    const auto best = std::max_element(first, first + config.vocabSize);
    std::ostringstream out;
    out << "Dev Orus 1.1 - experimental model\nTiny Transformer forward pass completed.\n"
        << "Sequence length: " << ids.size() << "\n"
        << "Architecture: 1 layer; 4 heads; width 32; SwiGLU 64\n"
        << "Logits: " << logits.size() << " values\n"
        << "Highest final-position logit: token " << std::distance(first, best)
        << " (score " << *best << ").\n\n"
        << "Research demo only: weights are freshly randomized at startup. "
        << "This is NOT a trained model and its output is not meaningful chat.";
    return env->NewStringUTF(out.str().c_str());
}


// First usable offline baseline: train a local character-ngram retrieval model
// from the bundled prompt/response corpus. This is intentionally not described
// as a generative Transformer or as a trained language model.
#include <cctype>
#include <cmath>
#include <unordered_map>

namespace {
struct RetrievalDocument {
    std::string answer;
    std::unordered_map<std::string, float> weights;
    float norm = 0.0f;
};
std::vector<RetrievalDocument> gRetrievalDocs;
std::unordered_map<std::string, float> gRetrievalIdf;
bool gRetrievalReady = false;

std::string normalizeAscii(std::string s) {
    for (char& c : s) {
        const unsigned char u = static_cast<unsigned char>(c);
        if (u < 128) c = static_cast<char>(std::tolower(u));
    }
    return s;
}
std::unordered_map<std::string, float> makeNgrams(const std::string& input) {
    const std::string s = normalizeAscii(input);
    std::unordered_map<std::string, float> terms;
    // UTF-8 byte n-grams preserve matching across English, Hindi and Urdu.
    for (std::size_t n = 2; n <= 4; ++n) {
        if (s.size() < n) continue;
        for (std::size_t i = 0; i + n <= s.size(); ++i) {
            const std::string gram = s.substr(i, n);
            bool whitespaceOnly = true;
            for (unsigned char c : gram) if (!std::isspace(c)) { whitespaceOnly = false; break; }
            if (!whitespaceOnly) terms[gram] += 1.0f;
        }
    }
    return terms;
}
}
extern "C" JNIEXPORT jstring JNICALL
Java_com_devai_MainActivity_nativeTrainCorpus(JNIEnv* env, jobject, jstring corpus) {
    if (!corpus) return env->NewStringUTF("Corpus unavailable.");
    const char* chars = env->GetStringUTFChars(corpus, nullptr);
    if (!chars) return env->NewStringUTF("Could not read corpus.");
    const std::string raw(chars);
    env->ReleaseStringUTFChars(corpus, chars);
    std::vector<RetrievalDocument> docs;
    std::vector<std::unordered_map<std::string, float>> rawTerms;
    std::unordered_map<std::string, int> documentFrequency;
    std::istringstream lines(raw);
    std::string line;
    while (std::getline(lines, line)) {
        const std::size_t split = line.find('\t');
        if (split == std::string::npos || split == 0 || split + 1 >= line.size()) continue;
        const std::string prompt = line.substr(0, split);
        const std::string answer = line.substr(split + 1);
        auto terms = makeNgrams(prompt);
        if (terms.empty()) continue;
        for (const auto& term : terms) ++documentFrequency[term.first];
        rawTerms.push_back(std::move(terms));
        RetrievalDocument doc;
        doc.answer = answer;
        docs.push_back(std::move(doc));
    }
    if (docs.empty()) return env->NewStringUTF("No valid prompt/response records found.");
    std::unordered_map<std::string, float> idf;
    const float count = static_cast<float>(docs.size());
    for (const auto& item : documentFrequency)
        idf[item.first] = std::log(1.0f + (count + 1.0f) / (item.second + 1.0f));
    for (std::size_t i = 0; i < docs.size(); ++i) {
        double norm = 0.0;
        for (const auto& term : rawTerms[i]) {
            const float weight = (1.0f + std::log(term.second)) * idf[term.first];
            docs[i].weights[term.first] = weight;
            norm += static_cast<double>(weight) * weight;
        }
        docs[i].norm = static_cast<float>(std::sqrt(norm));
    }
    {
        std::lock_guard<std::mutex> lock(gMutex);
        gRetrievalDocs.swap(docs);
        gRetrievalIdf.swap(idf);
        gRetrievalReady = true;
    }
    std::ostringstream out;
    out << "Offline retrieval training complete.\\nRecords learned: " << gRetrievalDocs.size()
        << "\\nMethod: character n-gram TF-IDF\\nNo external model or network used."
        << "\\nThis baseline retrieves learned examples; it does not yet generate novel text.";
    return env->NewStringUTF(out.str().c_str());
}
extern "C" JNIEXPORT jstring JNICALL
Java_com_devai_MainActivity_nativeAsk(JNIEnv* env, jobject, jstring question) {
    if (!question) return env->NewStringUTF("Please type a message.");
    const char* chars = env->GetStringUTFChars(question, nullptr);
    if (!chars) return env->NewStringUTF("I couldn't read that message.");
    const std::string query(chars);
    env->ReleaseStringUTFChars(question, chars);
    const auto terms = makeNgrams(query);
    if (terms.empty()) return env->NewStringUTF("Please write a little more so I can understand your question.");
    std::lock_guard<std::mutex> lock(gMutex);
    if (!gRetrievalReady || gRetrievalDocs.empty())
        return env->NewStringUTF("My offline knowledge is not loaded yet. Please restart DevAI and try again.");
    std::unordered_map<std::string, float> qWeights;
    double qNorm2 = 0.0;
    for (const auto& term : terms) {
        const auto found = gRetrievalIdf.find(term.first);
        if (found == gRetrievalIdf.end()) continue;
        const float w = (1.0f + std::log(term.second)) * found->second;
        qWeights[term.first] = w;
        qNorm2 += static_cast<double>(w) * w;
    }
    const float qNorm = static_cast<float>(std::sqrt(qNorm2));
    if (qNorm <= 0.0f) return env->NewStringUTF("I don't have a close match in my current offline knowledge yet.");
    float bestScore = 0.0f;
    std::size_t bestIndex = 0;
    for (std::size_t i = 0; i < gRetrievalDocs.size(); ++i) {
        const auto& doc = gRetrievalDocs[i];
        double dot = 0.0;
        for (const auto& term : qWeights) {
            const auto found = doc.weights.find(term.first);
            if (found != doc.weights.end()) dot += static_cast<double>(term.second) * found->second;
        }
        const float score = doc.norm > 0.0f ? static_cast<float>(dot / (qNorm * doc.norm)) : 0.0f;
        if (score > bestScore) { bestScore = score; bestIndex = i; }
    }
    if (bestScore < 0.10f)
        return env->NewStringUTF("I don't have a reliable match for that yet. My current offline knowledge is limited, and I don't want to guess.");
    const std::string& answer = gRetrievalDocs[bestIndex].answer;
    return env->NewStringUTF(answer.c_str());
}
