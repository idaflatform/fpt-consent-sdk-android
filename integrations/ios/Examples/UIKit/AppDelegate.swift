import UIKit
import SwiftUI
import ConsentCmp

@main
final class AppDelegate: UIResponder, UIApplicationDelegate {
    var window:UIWindow?
    func application(_ application:UIApplication,didFinishLaunchingWithOptions launchOptions:[UIApplication.LaunchOptionsKey:Any]?)->Bool {
        window=UIWindow(frame:UIScreen.main.bounds)
        window?.rootViewController=UINavigationController(rootViewController:ExampleController())
        window?.makeKeyAndVisible();return true
    }
}
final class ExampleController:UIViewController {
    private let key=UITextField(),origin=UITextField(),email=UITextField(),message=UILabel()
    private var cmp:ConsentCmp?,model:ConsentFormModel?,host:EmbeddedHost?
    private let stack=UIStackView()
    private var busy=false
    override func viewDidLoad(){
        super.viewDidLoad();title="CMP UIKit";view.backgroundColor = .systemBackground
        let scroll=UIScrollView();scroll.translatesAutoresizingMaskIntoConstraints=false;view.addSubview(scroll)
        stack.axis = .vertical;stack.spacing=16;stack.translatesAutoresizingMaskIntoConstraints=false;scroll.addSubview(stack)
        NSLayoutConstraint.activate([scroll.topAnchor.constraint(equalTo:view.safeAreaLayoutGuide.topAnchor),scroll.bottomAnchor.constraint(equalTo:view.bottomAnchor),scroll.leadingAnchor.constraint(equalTo:view.leadingAnchor),scroll.trailingAnchor.constraint(equalTo:view.trailingAnchor),stack.topAnchor.constraint(equalTo:scroll.contentLayoutGuide.topAnchor,constant:16),stack.bottomAnchor.constraint(equalTo:scroll.contentLayoutGuide.bottomAnchor,constant:-16),stack.leadingAnchor.constraint(equalTo:scroll.contentLayoutGuide.leadingAnchor,constant:16),stack.trailingAnchor.constraint(equalTo:scroll.contentLayoutGuide.trailingAnchor,constant:-16),stack.widthAnchor.constraint(equalTo:scroll.frameLayoutGuide.widthAnchor,constant:-32)])
        key.placeholder="Integration key";origin.text="https://cmp.biznext.vn";email.text="demo@example.invalid";message.numberOfLines=0
        [key,origin,email].forEach { $0.borderStyle = .roundedRect;stack.addArrangedSubview($0) }
        button("Cấu hình và nhúng form",#selector(configure))
        button("Gửi consent trước khi đăng ký",#selector(submit))
        button("Mở consent độc lập",#selector(open))
        button("Đăng xuất",#selector(clear))
        stack.addArrangedSubview(message)
    }
    private func button(_ title:String,_ action:Selector){let b=UIButton(type:.system);b.setTitle(title,for:.normal);b.addTarget(self,action:action,for:.touchUpInside);stack.addArrangedSubview(b)}
    @objc private func configure(){
        guard !busy,let code=key.text,!code.trimmingCharacters(in:.whitespacesAndNewlines).isEmpty,let url=origin.text,URL(string:url)?.host != nil else {message.text="Kiểm tra key và URL";return}
        if let old=host {old.willMove(toParent:nil);old.view.removeFromSuperview();old.removeFromParent()}
        let sdk=ConsentCmp(options:ConsentOptions(codeConfig:code,baseUrl:url));cmp=sdk
        let model=ConsentFormModel(cmp:sdk);self.model=model
        let host=EmbeddedHost(rootView:ConsentFormView(model:model,embeddedInForm:true));self.host=host
        addChild(host);stack.insertArrangedSubview(host.view,at:4);host.didMove(toParent:self)
        host.height=host.view.heightAnchor.constraint(equalToConstant:180);host.height?.isActive=true
        Task {await model.load()}
    }
    @objc private func submit(){
        guard !busy,let cmp=cmp,let model=model,model.config?.isActive == true,model.validateRequired() else {message.text="Form chưa sẵn sàng";return}
        busy=true
        Task { @MainActor in defer{busy=false};do{var state=model.currentState;let sent=try await cmp.submit(&state,values:["Email":email.text ?? ""]);message.text="Đã gửi consentId=\(sent.consentId ?? state.consentId ?? "")"}catch{message.text=error.localizedDescription}}
    }
    @objc private func open(){guard !busy,let cmp=cmp else{return};present(ConsentViewController(cmp:cmp,values:["Email":email.text ?? ""],onError:{[weak self] in self?.message.text=$0.message},onCompleted:{[weak self] state,result in self?.message.text="Đã gửi consentId=\(result.consentId ?? state.consentId ?? "")"}),animated:true)}
    @objc private func clear(){guard !busy else{return};cmp?.clear();if let model=model{Task {await model.load()}};message.text="Đã xóa quyết định"}
}
final class EmbeddedHost:UIHostingController<ConsentFormView> {
 var height:NSLayoutConstraint?
 override func viewDidLayoutSubviews(){
  super.viewDidLayoutSubviews()
  guard view.bounds.width>0 else{return}
  let measured=sizeThatFits(in:CGSize(width:view.bounds.width,height:.greatestFiniteMagnitude)).height
  if measured.isFinite,measured>0,abs((height?.constant ?? 0)-measured)>0.5 {height?.constant=ceil(measured)}
 }
}
