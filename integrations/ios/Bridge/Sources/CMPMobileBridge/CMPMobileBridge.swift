import Foundation
import UIKit
import SwiftUI
import Combine
import ConsentCmp

/// Objective-C-compatible entry point for native bindings and framework plugins.
@objc(CMPMobileBridge)
public final class CMPMobileBridge:NSObject {
    private static var active=false
    private static var inFlight=false
    private static var cmp:ConsentCmp?
    @objc public static func present(from parent:UIViewController,optionsJSON:String,completion:@escaping(String)->Void) {
        DispatchQueue.main.async {
            guard !active && !inFlight else {completion(error("busy","Consent screen or submission already active."));return}
            guard let data=optionsJSON.data(using:.utf8),let options=(try? JSONSerialization.jsonObject(with:data)) as? [String:Any],
                  let key=options["codeConfig"] as? String,!key.trimmingCharacters(in:.whitespacesAndNewlines).isEmpty,
                  let url=URLComponents(string:options["baseUrl"] as? String ?? ConsentOptions.defaultBaseUrl),["https","http"].contains(url.scheme ?? ""),url.host != nil,url.user == nil,url.password == nil,url.query == nil,url.fragment == nil,["","/"].contains(url.path)
            else {completion(error("invalid_arguments","Expected key and HTTP(S) origin."));return}
            guard parent.viewIfLoaded?.window != nil,parent.presentedViewController == nil else {completion(error("not_ready","Presenter is not available."));return}
            let sdk=ConsentCmp(options:ConsentOptions(codeConfig:key,baseUrl:url.string!));cmp=sdk;active=true
            let flow=ModalFlow(cmp:sdk,values:options["values"] as? [String:String] ?? [:],embedded:options["embedded"] as? Bool ?? false)
            flow.onBusy={inFlight=$0}
            let host=FlowController(flow:flow)
            flow.finished={ [weak host] json in active=false;completion(json);host?.dismiss(animated:true) }
            parent.present(host,animated:true)
        }
    }
    @objc public static func clear()->Bool {guard Thread.isMainThread,!active,!inFlight else{return false};cmp?.clear();return true}
    @objc public static func isGranted(_ key:String)->Bool {Thread.isMainThread && cmp?.isGranted(key)==true}
    @objc public static func savedConsentId()->String? {Thread.isMainThread ? cmp?.savedState()?.consentId:nil}
    fileprivate static func encode(_ object:[String:Any])->String {String(data:(try? JSONSerialization.data(withJSONObject:object)) ?? Data(),encoding:.utf8) ?? "{}"}
    fileprivate static func error(_ code:String,_ message:String)->String {encode(["ok":false,"error":["code":code,"message":message]])}
}
private final class ModalFlow:ObservableObject {
    let cmp:ConsentCmp
    let model:ConsentFormModel
    let embedded:Bool
    @Published var email:String
    @Published var phone:String
    @Published var busy=false
    @Published var message=""
    var values:[String:String]
    var finished:((String)->Void)?
    var onBusy:((Bool)->Void)?
    private var done=false
    init(cmp:ConsentCmp,values:[String:String],embedded:Bool){self.cmp=cmp;model=ConsentFormModel(cmp:cmp);self.values=values;self.embedded=embedded;email=values["Email"] ?? "";phone=values["PhoneNumber"] ?? ""}
    func finish(_ json:String){guard !done else{return};done=true;finished?(json)}
    func cancel(){finish(CMPMobileBridge.error("cancelled","Screen closed; an in-flight server request may still complete."))}
    func submit(){
        guard !busy,!done,model.config?.isActive == true,model.validateRequired() else{return}
        busy=true;onBusy?(true);var sentValues=values;if embedded{sentValues["Email"]=email;sentValues["PhoneNumber"]=phone}
        Task { @MainActor in defer{busy=false;onBusy?(false)};do{var state=model.currentState;let sent=try await cmp.submit(&state,values:sentValues);finish(CMPMobileBridge.encode(["ok":sent.ok,"consentId":sent.consentId ?? state.consentId ?? ""]))}catch{message=error.localizedDescription}}
    }
}
private struct FlowScreen:View {
    @ObservedObject var flow:ModalFlow
    @ObservedObject var model:ConsentFormModel
    var body:some View {
        NavigationView {ScrollView {VStack(alignment:.leading,spacing:16){
            if flow.embedded {TextField("Email",text:$flow.email).textFieldStyle(RoundedBorderTextFieldStyle());TextField("PhoneNumber",text:$flow.phone).textFieldStyle(RoundedBorderTextFieldStyle())}
            if model.config?.isActive == true {ConsentFormView(model:model,embeddedInForm:true)}
            else {Text(model.errorMessage ?? "Đang tải cấu hình");Button("Thử lại"){Task {await model.load()}}}
            Button(flow.embedded ? "Gửi consent trước khi đăng ký":"Lưu lựa chọn"){flow.submit()}.disabled(flow.busy || model.config?.isActive != true || !model.hasAllRequired)
            Text(flow.message)
        }.padding()}.navigationTitle("CMP Consent").navigationBarItems(trailing:Button("Đóng"){flow.cancel()})}
        .onAppear{Task {await model.load()}}
    }
}
private final class FlowController:UIHostingController<FlowScreen>,UIAdaptivePresentationControllerDelegate {
    let flow:ModalFlow
    init(flow:ModalFlow){self.flow=flow;super.init(rootView:FlowScreen(flow:flow,model:flow.model))}
    @available(*,unavailable) required init?(coder:NSCoder){fatalError("Use init(flow:)")}
    override func viewDidAppear(_ animated:Bool){super.viewDidAppear(animated);presentationController?.delegate=self}
    func presentationControllerDidDismiss(_ presentationController:UIPresentationController){flow.cancel()}
}
