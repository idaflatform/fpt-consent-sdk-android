import Capacitor
import CMPMobileBridge
@objc(CmpConsentPlugin)
public final class CmpConsentPlugin:CAPPlugin,CAPBridgedPlugin {
 public let identifier="CmpConsentPlugin"
 public let jsName="CmpConsent"
 public let pluginMethods:[CAPPluginMethod]=[CAPPluginMethod(name:"present",returnType:CAPPluginReturnPromise),CAPPluginMethod(name:"clear",returnType:CAPPluginReturnPromise)]
 @objc func present(_ call:CAPPluginCall){DispatchQueue.main.async{guard let parent=self.bridge?.viewController else{call.reject("Presenter not ready");return};CMPMobileBridge.present(from:parent,optionsJSON:call.getString("options") ?? "{}"){call.resolve(["json":$0])}}}
 @objc func clear(_ call:CAPPluginCall){DispatchQueue.main.async{call.resolve(["cleared":CMPMobileBridge.clear()])}}
}
