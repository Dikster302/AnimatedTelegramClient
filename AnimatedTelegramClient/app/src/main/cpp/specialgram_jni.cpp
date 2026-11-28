#include <jni.h>
#include "SpecialgramCore.h"

extern "C" JNIEXPORT jstring JNICALL
Java_com_dikster_specialgram_NativeCore_nativeSanitizeDiagnosticRequest(
        JNIEnv* env, jclass, jstring request) {
    if (request == nullptr) return env->NewStringUTF("UnknownRequest");
    const char* chars = env->GetStringUTFChars(request, nullptr);
    if (chars == nullptr) return env->NewStringUTF("UnknownRequest");
    const std::string safe = specialgram::sanitizeDiagnosticRequest(chars);
    env->ReleaseStringUTFChars(request, chars);
    return env->NewStringUTF(safe.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_dikster_specialgram_NativeCore_nativeEncodeVideoShape(
        JNIEnv* env, jclass, jstring shape) {
    if (shape == nullptr) return env->NewStringUTF("");
    const char* chars = env->GetStringUTFChars(shape, nullptr);
    if (chars == nullptr) return env->NewStringUTF("");
    const std::string encoded = specialgram::encodeVideoShape(chars);
    env->ReleaseStringUTFChars(shape, chars);
    return env->NewStringUTF(encoded.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_dikster_specialgram_NativeCore_nativeDecodeVideoShape(
        JNIEnv* env, jclass, jstring caption) {
    if (caption == nullptr) return env->NewStringUTF("circle");
    const char* chars = env->GetStringUTFChars(caption, nullptr);
    if (chars == nullptr) return env->NewStringUTF("circle");
    const std::string decoded = specialgram::decodeVideoShape(chars);
    env->ReleaseStringUTFChars(caption, chars);
    return env->NewStringUTF(decoded.c_str());
}
