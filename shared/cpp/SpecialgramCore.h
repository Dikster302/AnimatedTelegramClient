#pragma once

#include <string>

namespace specialgram {

// Platform-independent utility shared by Android JNI and future iOS bindings.
std::string sanitizeDiagnosticRequest(const std::string& request);

}  // namespace specialgram
