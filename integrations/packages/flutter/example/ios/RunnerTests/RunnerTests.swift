import Flutter
import UIKit
import XCTest


@testable import cmp_consent

class RunnerTests: XCTestCase {
  func testMissingConfigurationReturnsErrorWithoutAccessingSharedSingleton() {
    let plugin = MobileConsentPlugin()
    let call = FlutterMethodCall(methodName: "clear", arguments: nil)
    let resultExpectation = expectation(description: "result must complete")
    plugin.handle(call) { result in
      XCTAssertEqual((result as? FlutterError)?.code, "not_configured")
      resultExpectation.fulfill()
    }
    waitForExpectations(timeout: 1)
  }
  func testInvalidOriginIsRejected() {
    let plugin=MobileConsentPlugin()
    let call=FlutterMethodCall(methodName:"configure",arguments:["codeConfig":"demo","baseUrl":"https://user:secret@example.invalid"])
    let completed=expectation(description:"invalid arguments")
    plugin.handle(call){result in XCTAssertEqual((result as? FlutterError)?.code,"invalid_arguments");completed.fulfill()}
    waitForExpectations(timeout:1)
  }
}
