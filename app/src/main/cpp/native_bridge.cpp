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
    out << "DevAI byte-level BPE tokenizer\nStatus: "
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
    out << "Tiny Transformer forward pass completed.\n"
        << "Sequence length: " << ids.size() << "\n"
        << "Architecture: 1 layer; 4 heads; width 32; SwiGLU 64\n"
        << "Logits: " << logits.size() << " values\n"
        << "Highest final-position logit: token " << std::distance(first, best)
        << " (score " << *best << ").\n\n"
        << "Research demo only: weights are freshly randomized at startup. "
        << "This is NOT a trained model and its output is not meaningful chat.";
    return env->NewStringUTF(out.str().c_str());
}
