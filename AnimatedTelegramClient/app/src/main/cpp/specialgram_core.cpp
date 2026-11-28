#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_dikster_specialgram_NativeCore_nativeSanitizeDiagnosticRequest(
        JNIEnv* env, jclass, jstring request) {
    if (request == nullptr) return env->NewStringUTF("UnknownRequest");
    const char* chars = env->GetStringUTFChars(request, nullptr);
    if (chars == nullptr) return env->NewStringUTF("UnknownRequest");
    std::string safe;
    safe.reserve(64);
    for (const unsigned char* p = reinterpret_cast<const unsigned char*>(chars);
         *p != 0 && safe.size() < 64; ++p) {
        const unsigned char c = *p;
        if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') ||
            (c >= '0' && c <= '9') || c == '_') safe.push_back(static_cast<char>(c));
    }
    env->ReleaseStringUTFChars(request, chars);
    if (safe.empty()) safe = "UnknownRequest";
    return env->NewStringUTF(safe.c_str());
}
