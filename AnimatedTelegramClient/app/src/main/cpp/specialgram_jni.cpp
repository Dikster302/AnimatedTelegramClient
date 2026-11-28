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
