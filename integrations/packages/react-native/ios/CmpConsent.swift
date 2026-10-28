import Foundation
import React
import CMPMobileBridge
@objc(CmpConsent)
final class CmpConsent:NSObject {
 @objc static func requiresMainQueueSetup()->Bool {true}
 @objc func present(_ options:String,resolver resolve:@escaping RCTPromiseResolveBlock,rejecter reject:@escaping RCTPromiseRejectBlock){
  DispatchQueue.main.async {guard let parent=RCTPresentedViewController() else {reject("not_ready","Presenter unavailable",nil);return};CMPMobileBridge.present(from:parent,optionsJSON:options){resolve($0)}}
 }
 @objc func clear(_ resolve:@escaping RCTPromiseResolveBlock,rejecter reject:@escaping RCTPromiseRejectBlock){DispatchQueue.main.async{resolve(CMPMobileBridge.clear())}}
 @objc func isGranted(_ key:String,resolver resolve:@escaping RCTPromiseResolveBlock,rejecter reject:@escaping RCTPromiseRejectBlock){DispatchQueue.main.async{resolve(CMPMobileBridge.isGranted(key))}}
 @objc func savedConsentId(_ resolve:@escaping RCTPromiseResolveBlock,rejecter reject:@escaping RCTPromiseRejectBlock){DispatchQueue.main.async{resolve(CMPMobileBridge.savedConsentId())}}
}
