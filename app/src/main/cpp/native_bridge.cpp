#include <jni.h>
#include <iomanip>
#include <sstream>
#include <string>

static std::string toHex(const std::string& input) {
    std::ostringstream out;
    out << std::hex << std::setfill('0');
    for (unsigned char byte : input) {
        out << std::setw(2) << static_cast<unsigned int>(byte);
    }
    return out.str();
}

static bool fromHex(const std::string& hex, std::string& output) {
    if (hex.size() % 2 != 0) return false;

    output.clear();
    for (std::size_t i = 0; i < hex.size(); i += 2) {
        unsigned int value = 0;
        std::istringstream part(hex.substr(i, 2));
        part >> std::hex >> value;
        if (part.fail() || value > 255) return false;
        output.push_back(static_cast<char>(value));
    }
    return true;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_devai_MainActivity_nativeTokenizerInfo(JNIEnv* env, jobject) {
    const std::string info =
        "DevAI native core loaded.\n"
        "Prototype: reversible byte encoding.\n"
        "Byte values: 0..255 (256 possible IDs).\n"
        "This is not BPE, a trained tokenizer, or a language model.";
    return env->NewStringUTF(info.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_devai_MainActivity_nativeEncodeAsHex(JNIEnv* env, jobject, jstring input) {
    if (input == nullptr) return env->NewStringUTF("");

    const char* chars = env->GetStringUTFChars(input, nullptr);
    if (chars == nullptr) return env->NewStringUTF("");

    const std::string bytes(chars);
    env->ReleaseStringUTFChars(input, chars);

    const std::string hex = toHex(bytes);
    return env->NewStringUTF(hex.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_devai_MainActivity_nativeDecodeHex(JNIEnv* env, jobject, jstring input) {
    if (input == nullptr) return env->NewStringUTF("");

    const char* chars = env->GetStringUTFChars(input, nullptr);
    if (chars == nullptr) return env->NewStringUTF("");

    const std::string hex(chars);
    env->ReleaseStringUTFChars(input, chars);

    std::string decoded;
    if (!fromHex(hex, decoded)) {
        return env->NewStringUTF("Invalid hexadecimal input.");
    }
    return env->NewStringUTF(decoded.c_str());
}
