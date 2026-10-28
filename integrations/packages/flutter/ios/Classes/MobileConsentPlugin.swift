import Flutter
import UIKit
import SwiftUI
import Combine
import ConsentCmp

public final class MobileConsentPlugin:NSObject,FlutterPlugin {
    fileprivate var cmp:ConsentCmp?
    private var configuration:String?
    fileprivate var submissions=0
    fileprivate var forms:[Int64:ConsentPlatformView]=[:]
    public static func register(with registrar:FlutterPluginRegistrar) {
        let plugin=MobileConsentPlugin()
        registrar.addMethodCallDelegate(plugin,channel:FlutterMethodChannel(name:"vn.fpt.cmp/control",binaryMessenger:registrar.messenger()))
        registrar.register(ConsentFactory(plugin:plugin,messenger:registrar.messenger(),parent:registrar.viewController()),withId:"vn.fpt.cmp/form")
    }
    public func handle(_ call:FlutterMethodCall,result:@escaping FlutterResult) {
        let args=call.arguments as? [String:Any] ?? [:]
        switch call.method {
        case "configure":
            guard forms.isEmpty,submissions==0 else {result(FlutterError(code:"busy",message:"Dispose forms and wait for pending submissions before configuring.",details:nil));return}
            guard let key=args["codeConfig"] as? String,!key.trimmingCharacters(in:.whitespacesAndNewlines).isEmpty,
                  let origin=args["baseUrl"] as? String,let url=URLComponents(string:origin),["https","http"].contains(url.scheme ?? ""),url.host != nil,url.user == nil,url.password == nil,url.query == nil,url.fragment == nil,["","/"].contains(url.path)
            else {result(FlutterError(code:"invalid_arguments",message:"Expected key and HTTP(S) origin.",details:nil));return}
            let configuration=key+"\n"+origin
            if self.configuration != nil && self.configuration != configuration {cmp?.clear()}
            self.configuration=configuration
            cmp=ConsentCmp(options:ConsentOptions(codeConfig:key,baseUrl:origin));result(nil)
        case "clear":
            guard let cmp=cmp else {result(FlutterError(code:"not_configured",message:"Configure CMP first.",details:nil));return}
            guard submissions==0,!forms.values.contains(where:{$0.busy}) else {result(FlutterError(code:"busy",message:"Submission in progress.",details:nil));return}
            cmp.clear();forms.values.forEach{$0.reload()};result(nil)
        case "savedConsentId":result(cmp?.savedState()?.consentId)
        case "isGranted":result(cmp?.isGranted(args["purposeKey"] as? String ?? "") ?? false)
        default:result(FlutterMethodNotImplemented)
        }
    }
}
private final class ConsentFactory:NSObject,FlutterPlatformViewFactory {
    let plugin:MobileConsentPlugin
    let messenger:FlutterBinaryMessenger
    weak var parent:UIViewController?
    init(plugin:MobileConsentPlugin,messenger:FlutterBinaryMessenger,parent:UIViewController?){self.plugin=plugin;self.messenger=messenger;self.parent=parent}
    func createArgsCodec()->FlutterMessageCodec & NSObjectProtocol {FlutterStandardMessageCodec.sharedInstance()}
    func create(withFrame frame:CGRect,viewIdentifier id:Int64,arguments args:Any?)->FlutterPlatformView {
        let form=ConsentPlatformView(frame:frame,id:id,args:args as? [String:Any] ?? [:],plugin:plugin,messenger:messenger,parent:parent)
        plugin.forms[id]=form;return form
    }
}
private struct HeightPreference:PreferenceKey {
    static var defaultValue:CGFloat=0
    static func reduce(value:inout CGFloat,nextValue:()->CGFloat){value=nextValue()}
}
private struct MeasuredForm:View {
    @ObservedObject var model:ConsentFormModel
    let embedded:Bool
    let values:[String:String]
    let report:(CGFloat)->Void
    var body:some View {
        ConsentFormView(model:model,embeddedInForm:embedded,values:values)
            .fixedSize(horizontal:false,vertical:true)
            .background(GeometryReader{geometry in Color.clear.preference(key:HeightPreference.self,value:geometry.size.height)})
            .onPreferenceChange(HeightPreference.self,perform:report)
    }
}
fileprivate final class ConsentPlatformView:NSObject,FlutterPlatformView {
    let container:UIView
    let channel:FlutterMethodChannel
    weak var plugin:MobileConsentPlugin?
    let id:Int64
    private var model:ConsentFormModel?
    private var host:UIHostingController<MeasuredForm>?
    private var values:[String:String]
    private let embedded:Bool
    private var subscription:AnyCancellable?
    private var height:CGFloat=180
    private var disposed=false
    private var loading=false
    private var pending:FlutterResult?
    private var loadTask:Task<Void,Never>?
    var busy:Bool {pending != nil || model?.isSubmitting == true}
    init(frame:CGRect,id:Int64,args:[String:Any],plugin:MobileConsentPlugin,messenger:FlutterBinaryMessenger,parent:UIViewController?) {
        self.id=id;self.plugin=plugin;container=UIView(frame:frame)
        values=args["values"] as? [String:String] ?? [:];embedded=args["embedded"] as? Bool ?? true
        channel=FlutterMethodChannel(name:"vn.fpt.cmp/form/\(id)",binaryMessenger:messenger)
        super.init()
        if let cmp=plugin.cmp {
            let model=ConsentFormModel(cmp:cmp);self.model=model
            let host=UIHostingController(rootView:makeRoot(model));self.host=host
            parent?.addChild(host);host.view.translatesAutoresizingMaskIntoConstraints=false;container.addSubview(host.view)
            NSLayoutConstraint.activate([host.view.leadingAnchor.constraint(equalTo:container.leadingAnchor),host.view.trailingAnchor.constraint(equalTo:container.trailingAnchor),host.view.topAnchor.constraint(equalTo:container.topAnchor)])
            host.didMove(toParent:parent)
            subscription=model.objectWillChange.sink { [weak self] _ in DispatchQueue.main.async {self?.reportStatus()} }
            model.onSubmitted={ [weak self] state,sent in
                guard let self=self,!self.disposed else{return}
                self.channel.invokeMethod("onSubmitted",arguments:["ok":sent.ok,"consentId":sent.consentId ?? state.consentId ?? ""])
            }
        }
        channel.setMethodCallHandler { [weak self] call,result in
            guard let self=self else {result(FlutterError(code:"disposed",message:"Form disposed.",details:nil));return}
            self.handle(call,result:result)
        }
        reload()
    }
    private func makeRoot(_ model:ConsentFormModel)->MeasuredForm {
        MeasuredForm(model:model,embedded:embedded,values:values,report:{[weak self] height in
            guard let self=self,!self.disposed,height.isFinite,height>0,abs(self.height-height)>0.5 else{return}
            self.height=ceil(height);self.channel.invokeMethod("onHeight",arguments:Double(self.height))
        })
    }
    func view()->UIView {container}
    private func status()->[String:Any] {
        guard !disposed else{return ["state":"disposed","height":Double(height)]}
        guard let model=model else{return ["state":"failed","code":"not_configured","message":"Configure CMP first.","height":Double(height)]}
        var state="loading"
        if !loading && !model.isLoading {state=model.config == nil ? "failed" : (model.config?.isActive == true ? "ready":"inactive")}
        return ["state":state,"allRequiredGranted":model.hasAllRequired,"message":model.errorMessage ?? "","height":Double(height)]
    }
    private func reportStatus(){if !disposed{channel.invokeMethod("onStatus",arguments:status())}}
    func reload(){
        guard !disposed,let model=model else{reportStatus();return}
        loading=true;reportStatus();loadTask?.cancel()
        loadTask=Task { @MainActor [weak self] in await model.load();guard !Task.isCancelled else{return};self?.loading=false;self?.reportStatus() }
    }
    private func handle(_ call:FlutterMethodCall,result:@escaping FlutterResult) {
        let args=call.arguments as? [String:Any] ?? [:]
        if disposed && call.method != "dispose" {result(FlutterError(code:"disposed",message:"Form disposed.",details:nil));return}
        switch call.method {
        case "status":result(status())
        case "values":values=args["values"] as? [String:String] ?? [:];if let model=model{host?.rootView=makeRoot(model)};result(nil)
        case "reload":if busy{result(FlutterError(code:"busy",message:"Submission in progress.",details:nil))}else{reload();result(nil)}
        case "submit":
            guard !busy else{result(FlutterError(code:"busy",message:"Submission in progress.",details:nil));return}
            guard let model=model,let cmp=plugin?.cmp,status()["state"] as? String == "ready",model.validateRequired() else{result(FlutterError(code:"not_ready",message:"Form not ready or required consent missing.",details:nil));return}
            pending=result;let sentValues=args["values"] as? [String:String] ?? [:]
            let owner=plugin;owner?.submissions += 1
            Task { @MainActor [weak self] in
                defer {owner?.submissions -= 1}
                do {var state=model.currentState;let sent=try await cmp.submit(&state,values:sentValues)
                    guard let self=self else{return};let callback=self.pending;self.pending=nil
                    callback?(["ok":sent.ok,"consentId":sent.consentId ?? state.consentId ?? ""])
                } catch {
                    guard let self=self else{return};let callback=self.pending;self.pending=nil;model.errorMessage=error.localizedDescription
                    callback?(FlutterError(code:"submit_failed",message:error.localizedDescription,details:["httpStatus":(error as? ConsentError)?.httpStatus ?? 0]))
                }
            }
        case "dispose":dispose();result(nil)
        default:result(FlutterMethodNotImplemented)
        }
    }
    private func dispose(){
        guard !disposed else{return};disposed=true;loadTask?.cancel();subscription?.cancel()
        let callback=pending;pending=nil;callback?(FlutterError(code:"disposed",message:"Form closed before submission completed.",details:nil))
        channel.setMethodCallHandler(nil);host?.willMove(toParent:nil);host?.view.removeFromSuperview();host?.removeFromParent();host=nil;plugin?.forms.removeValue(forKey:id)
    }
    deinit { loadTask?.cancel();subscription?.cancel() }
}
