#include "SpecialgramCore.h"

namespace specialgram {

std::string sanitizeDiagnosticRequest(const std::string& request) {
    std::string safe;
    safe.reserve(64);
    for (unsigned char value : request) {
        if (safe.size() >= 64) break;
        if ((value >= 'a' && value <= 'z') || (value >= 'A' && value <= 'Z') ||
            (value >= '0' && value <= '9') || value == '_') {
            safe.push_back(static_cast<char>(value));
        }
    }
    return safe.empty() ? "UnknownRequest" : safe;
}

}  // namespace specialgram
