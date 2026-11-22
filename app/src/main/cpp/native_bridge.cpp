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


// Gemma-backed Dev Orus 1.1 inference through the official LiteRT-LM C API.
// The Java/XML layer owns model download and lifecycle; this JNI layer owns
// the native Engine and stateful Conversation.
#include "c/engine.h"
#include "c/conversation.h"
#include "c/error_reporter.h"

namespace {
std::mutex gGemmaMutex;
LiteRtLmEngine* gGemmaEngine = nullptr;
LiteRtLmConversation* gGemmaConversation = nullptr;

std::string gemmaLastError(const char* operation, LiteRtLmStatusCode status) {
    const char* detail = litert_lm_get_last_error_message();
    std::ostringstream out;
    out << operation << " failed (status " << static_cast<int>(status) << ")";
    if (detail && *detail) out << ": " << detail;
    return out.str();
}

std::string jsonEscape(const std::string& value) {
    std::ostringstream out;
    for (unsigned char c : value) {
        switch (c) {
            case '"': out << '\\' << '"'; break;
            case '\\': out << '\\' << '\\'; break;
            case '\b': out << "\\b"; break;
            case '\f': out << "\\f"; break;
            case '\n': out << "\\n"; break;
            case '\r': out << "\\r"; break;
            case '\t': out << "\\t"; break;
            default:
                if (c < 0x20) {
                    const char* hex = "0123456789abcdef";
                    out << "\\u00" << hex[c >> 4] << hex[c & 0x0f];
                } else {
                    out << static_cast<char>(c);
                }
        }
    }
    return out.str();
}

void closeGemmaLocked() {
    if (gGemmaConversation) {
        litert_lm_conversation_delete(gGemmaConversation);
        gGemmaConversation = nullptr;
    }
    if (gGemmaEngine) {
        litert_lm_engine_delete(gGemmaEngine);
        gGemmaEngine = nullptr;
    }
}
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_devai_MainActivity_nativeInitGemma(JNIEnv* env, jobject, jstring modelPath) {
    if (!modelPath) return env->NewStringUTF("Model path is empty.");
    const char* pathChars = env->GetStringUTFChars(modelPath, nullptr);
    if (!pathChars) return env->NewStringUTF("Could not read model path.");
    const std::string path(pathChars);
    env->ReleaseStringUTFChars(modelPath, pathChars);

    std::lock_guard<std::mutex> lock(gGemmaMutex);
    closeGemmaLocked();

    LiteRtLmEngineSettings* settings = nullptr;
    LiteRtLmStatusCode status =
        litert_lm_engine_settings_create(path.c_str(), "cpu", nullptr, nullptr, &settings);
    if (status != kLiteRtLmStatusOk) {
        return env->NewStringUTF(gemmaLastError("LiteRT-LM settings", status).c_str());
    }

    status = litert_lm_engine_create(settings, &gGemmaEngine);
    litert_lm_engine_settings_delete(settings);
    if (status != kLiteRtLmStatusOk) {
        closeGemmaLocked();
        return env->NewStringUTF(gemmaLastError("Gemma engine initialization", status).c_str());
    }

    LiteRtLmSessionConfig* sessionConfig = nullptr;
    LiteRtLmConversationConfig* conversationConfig = nullptr;
    status = litert_lm_session_config_create(&sessionConfig);
    if (status == kLiteRtLmStatusOk) {
        status = litert_lm_session_config_set_max_output_tokens(sessionConfig, 512);
    }
    if (status == kLiteRtLmStatusOk) {
        status = litert_lm_conversation_config_create(&conversationConfig);
    }
    if (status == kLiteRtLmStatusOk) {
        status = litert_lm_conversation_config_set_session_config(
            conversationConfig, sessionConfig);
    }
    const char* systemMessage =
        "{\\"role\\":\\"system\\",\\"content\\":\\"You are Dev Orus 1.1, a helpful, "
        "honest, clear and respectful offline assistant. Answer in the language used by "
        "the user. Do not claim access to the internet. If unsure, say so.\\"}";
    if (status == kLiteRtLmStatusOk) {
        status = litert_lm_conversation_config_set_system_message(
            conversationConfig, systemMessage);
    }
    if (status == kLiteRtLmStatusOk) {
        status = litert_lm_conversation_create(
            gGemmaEngine, conversationConfig, &gGemmaConversation);
    }

    if (conversationConfig) litert_lm_conversation_config_delete(conversationConfig);
    if (sessionConfig) litert_lm_session_config_delete(sessionConfig);

    if (status != kLiteRtLmStatusOk) {
        const std::string error = gemmaLastError("Dev Orus conversation setup", status);
        closeGemmaLocked();
        return env->NewStringUTF(error.c_str());
    }
    return env->NewStringUTF("READY");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_devai_MainActivity_nativeGenerate(JNIEnv* env, jobject, jstring prompt) {
    if (!prompt) return env->NewStringUTF("Message is empty.");
    const char* promptChars = env->GetStringUTFChars(prompt, nullptr);
    if (!promptChars) return env->NewStringUTF("Could not read message.");
    const std::string text(promptChars);
    env->ReleaseStringUTFChars(prompt, promptChars);

    std::lock_guard<std::mutex> lock(gGemmaMutex);
    if (!gGemmaConversation) return env->NewStringUTF("Gemma model is not loaded.");
    const std::string message =
        "{\\"role\\":\\"user\\",\\"content\\":\\"" + jsonEscape(text) + "\\"}";
    LiteRtLmJsonResponse* response = nullptr;
    const LiteRtLmStatusCode status = litert_lm_conversation_send_message(
        gGemmaConversation, message.c_str(), nullptr, nullptr, &response);
    if (status != kLiteRtLmStatusOk) {
        return env->NewStringUTF(gemmaLastError("Dev Orus generation", status).c_str());
    }

    const char* responseJson = nullptr;
    const LiteRtLmStatusCode readStatus =
        litert_lm_json_response_get_string(response, &responseJson);
    std::string result = readStatus == kLiteRtLmStatusOk && responseJson
        ? responseJson : "{\\"error\\":\\"The model returned an empty response.\\"}";
    litert_lm_json_response_delete(response);
    return env->NewStringUTF(result.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_devai_MainActivity_nativeShutdownGemma(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lock(gGemmaMutex);
    closeGemmaLocked();
}
