import Foundation
import SpecialgramCore

/// Swift-facing API for the portable C++ core used by Specialgram clients.
public enum SpecialgramCoreBridge {
    public static func sanitizeDiagnosticRequest(_ request: String) -> String {
        request.withCString { value in
            guard let result = sg_sanitize_diagnostic_request(value) else {
                return "UnknownRequest"
            }
            return String(cString: result)
        }
    }

    public static func encodeVideoShape(_ shape: String) -> String {
        shape.withCString { value in
            guard let result = sg_encode_video_shape(value) else { return "" }
            return String(cString: result)
        }
    }

    public static func decodeVideoShape(_ caption: String) -> String {
        caption.withCString { value in
            guard let result = sg_decode_video_shape(value) else { return "circle" }
            return String(cString: result)
        }
    }
}
