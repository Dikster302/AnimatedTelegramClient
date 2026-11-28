#include "SpecialgramCoreC.h"
#include "SpecialgramCore.h"

#include <string>

namespace {
thread_local std::string result;
}

extern "C" const char* sg_sanitize_diagnostic_request(const char* request) {
    result = specialgram::sanitizeDiagnosticRequest(request == nullptr ? "" : request);
    return result.c_str();
}

extern "C" const char* sg_encode_video_shape(const char* shape) {
    result = specialgram::encodeVideoShape(shape == nullptr ? "circle" : shape);
    return result.c_str();
}

extern "C" const char* sg_decode_video_shape(const char* caption) {
    result = specialgram::decodeVideoShape(caption == nullptr ? "" : caption);
    return result.c_str();
}
