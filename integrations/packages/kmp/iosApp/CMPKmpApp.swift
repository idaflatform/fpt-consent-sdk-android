import SwiftUI
import SharedCmp
import CMPMobileBridge
final class IOSConsentPresenter:NSObject,NativeConsentPresenter {
 weak var parent:UIViewController?
 func present(optionsJson:String,onResult:@escaping(String)->KotlinUnit){
  guard let parent=parent else{_ = onResult("{\"ok\":false,\"error\":{\"code\":\"not_ready\"}}");return}
  CMPMobileBridge.present(from:parent,optionsJSON:optionsJson){_ = onResult($0)}
 }
}
@main struct CMPKmpApp:App {var body:some Scene {WindowGroup{KmpExample()}}}
struct KmpExample:UIViewControllerRepresentable {
 func makeUIViewController(context:Context)->UIViewController {KmpExampleController()}
 func updateUIViewController(_ controller:UIViewController,context:Context){}
}
final class KmpExampleController:UIViewController {
 let presenter=IOSConsentPresenter(),key=UITextField(),origin=UITextField(),result=UILabel()
 lazy var client=CmpClient(presenter:presenter)
 override func viewDidLoad(){super.viewDidLoad();view.backgroundColor = .systemBackground;presenter.parent=self
  let stack=UIStackView();stack.axis = .vertical;stack.spacing=16;stack.translatesAutoresizingMaskIntoConstraints=false;view.addSubview(stack)
  NSLayoutConstraint.activate([stack.topAnchor.constraint(equalTo:view.safeAreaLayoutGuide.topAnchor,constant:24),stack.leadingAnchor.constraint(equalTo:view.leadingAnchor,constant:16),stack.trailingAnchor.constraint(equalTo:view.trailingAnchor,constant:-16)])
  key.placeholder="Integration key";origin.text="https://cmp.biznext.vn";result.numberOfLines=0;stack.addArrangedSubview(key);stack.addArrangedSubview(origin)
  for embedded in [false,true]{let button=UIButton(type:.system);button.setTitle(embedded ? "Đăng ký native":"Consent độc lập",for:.normal);button.tag=embedded ? 1:0;button.addTarget(self,action:#selector(open(_:)),for:.touchUpInside);stack.addArrangedSubview(button)};stack.addArrangedSubview(result)
 }
 @objc func open(_ sender:UIButton){let options:[String:Any]=["codeConfig":key.text ?? "","baseUrl":origin.text ?? "","embedded":sender.tag==1,"values":["Email":"demo@example.invalid"]];let json=String(data:try! JSONSerialization.data(withJSONObject:options),encoding:.utf8)!;client.open(optionsJson:json){[weak self] json in self?.result.text=json;return KotlinUnit()}}
}
