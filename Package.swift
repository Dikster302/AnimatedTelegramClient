// swift-tools-version: 6.0
import PackageDescription

let package = Package(
    name: "SpecialgramShared",
    products: [
        .library(name: "SpecialgramShared", targets: ["SpecialgramCore", "SpecialgramSwift"])
    ],
    targets: [
        .target(
            name: "SpecialgramCore",
            path: "shared/cpp",
            publicHeadersPath: "."
        ),
        .target(
            name: "SpecialgramSwift",
            dependencies: ["SpecialgramCore"],
            path: "shared/swift",
            swiftSettings: [.interoperabilityMode(.Cxx)]
        ),
        .testTarget(
            name: "SpecialgramSwiftTests",
            dependencies: ["SpecialgramSwift"],
            path: "shared/swift-tests"
        )
    ],
    cxxLanguageStandard: .cxx17
)
