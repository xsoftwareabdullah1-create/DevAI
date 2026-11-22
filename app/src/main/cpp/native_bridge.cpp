#include <jni.h>
#include <mutex>
#include <sstream>
#include <string>
#include <vector>
#include "tokenizer.h"

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
        << "\nTokenizer only — no neural-network weights have been trained.";
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
