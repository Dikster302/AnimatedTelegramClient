import XCTest
@testable import SpecialgramSwift

final class SpecialgramCoreBridgeTests: XCTestCase {
    func testVideoShapesRoundTrip() {
        let shapes = ["circle", "square", "rounded", "heart", "star", "hexagon"]
        for shape in shapes {
            let encoded = SpecialgramCoreBridge.encodeVideoShape(shape)
            XCTAssertEqual(SpecialgramCoreBridge.decodeVideoShape(encoded), shape)
        }
    }

    func testUnknownVideoShapeFallsBackToCircle() {
        let encoded = SpecialgramCoreBridge.encodeVideoShape("unknown")
        XCTAssertEqual(SpecialgramCoreBridge.decodeVideoShape(encoded), "circle")
        XCTAssertEqual(SpecialgramCoreBridge.decodeVideoShape("plain caption"), "circle")
    }

    func testDiagnosticRequestIsSanitized() {
        XCTAssertEqual(
            SpecialgramCoreBridge.sanitizeDiagnosticRequest("Get_Chat-123"),
            "Get_Chat"
        )
    }
}
