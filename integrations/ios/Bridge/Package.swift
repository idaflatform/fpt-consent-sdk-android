// swift-tools-version:5.9
import PackageDescription
let package=Package(name:"CMPMobileBridge",platforms:[.iOS(.v14)],products:[.library(name:"CMPMobileBridge",type:.dynamic,targets:["CMPMobileBridge"])],dependencies:[.package(url:"https://github.com/idaplatform/fpt-consent-sdk-ios.git",from:"1.1.0")],targets:[.target(name:"CMPMobileBridge",dependencies:[.product(name:"ConsentCmp",package:"fpt-consent-sdk-ios")])])
