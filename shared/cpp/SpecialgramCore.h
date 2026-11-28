#pragma once
#include <string>
namespace specialgram {
std::string sanitizeDiagnosticRequest(const std::string& request);
std::string encodeVideoShape(const std::string& shape);
std::string decodeVideoShape(const std::string& caption);
}  // namespace specialgram
