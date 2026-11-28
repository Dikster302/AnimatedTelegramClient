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

std::string encodeVideoShape(const std::string& shape) {
    static const char* names[] = {"circle", "square", "rounded", "heart", "star", "hexagon"};
    int index = 0;
    for (int i = 0; i < 6; ++i) {
        if (shape == names[i]) { index = i; break; }
    }
    const char* bits = nullptr;
    switch (index) {
        case 1: bits = "001"; break;
        case 2: bits = "010"; break;
        case 3: bits = "011"; break;
        case 4: bits = "100"; break;
        case 5: bits = "101"; break;
        default: bits = "000"; break;
    }
    std::string result = "\xE2\x81\xA3";
    for (int i = 0; i < 3; ++i) {
        result += bits[i] == '1' ? "\xE2\x80\x8C" : "\xE2\x80\x8B";
    }
    result += "\xE2\x81\xA3";
    return result;
}

std::string decodeVideoShape(const std::string& caption) {
    const std::string separator = "\xE2\x81\xA3";
    const std::string one = "\xE2\x80\x8C";
    const std::string zero = "\xE2\x80\x8B";
    const std::size_t start = caption.find(separator);
    if (start == std::string::npos) return "circle";
    const std::size_t content = start + separator.size();
    const std::size_t end = caption.find(separator, content);
    if (end == std::string::npos || end - content != 9) return "circle";
    int index = 0;
    for (int i = 0; i < 3; ++i) {
        const std::string bit = caption.substr(content + static_cast<std::size_t>(i) * 3, 3);
        if (bit == one) index = (index << 1) | 1;
        else if (bit == zero) index <<= 1;
        else return "circle";
    }
    static const char* names[] = {"circle", "square", "rounded", "heart", "star", "hexagon"};
    return index < 6 ? names[index] : "circle";
}

}  // namespace specialgram
