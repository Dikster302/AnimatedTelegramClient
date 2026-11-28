#pragma once

#ifdef __cplusplus
extern "C" {
#endif

const char* sg_sanitize_diagnostic_request(const char* request);
const char* sg_encode_video_shape(const char* shape);
const char* sg_decode_video_shape(const char* caption);

#ifdef __cplusplus
}
#endif
