import SwiftUI
import ConsentCmp

@main
struct CMPExampleApp: App {
    var body: some Scene { WindowGroup { ConfigurationScreen() } }
}

struct ConfigurationScreen: View {
    @State private var key = ""
    @State private var url = "https://cmp.biznext.vn"
    @State private var cmp: ConsentCmp?
    var body: some View {
        NavigationView { VStack(spacing: 16) {
            TextField("Collection Point integration key", text: $key).textFieldStyle(RoundedBorderTextFieldStyle())
            TextField("CMP origin URL", text: $url).textFieldStyle(RoundedBorderTextFieldStyle())
            Button("Cấu hình") { cmp = ConsentCmp(options: ConsentOptions(codeConfig:key,baseUrl:url)) }
                .disabled(key.trimmingCharacters(in:.whitespacesAndNewlines).isEmpty || URL(string:url)?.host == nil)
            if let cmp = cmp { NavigationLink("Mở ví dụ", destination: RegistrationScreen(cmp:cmp).id(ObjectIdentifier(cmp))) }
        }.padding().navigationTitle("CMP SwiftUI") }
    }
}

struct RegistrationScreen: View {
    let cmp: ConsentCmp
    @StateObject private var model: ConsentFormModel
    @State private var email = "demo@example.invalid"
    @State private var message = ""
    @State private var busy = false
    @State private var standalone = false
    init(cmp: ConsentCmp) { self.cmp=cmp; _model=StateObject(wrappedValue:ConsentFormModel(cmp:cmp)) }
    var body: some View {
        ScrollView { VStack(alignment:.leading,spacing:16) {
            TextField("Email",text:$email).textFieldStyle(RoundedBorderTextFieldStyle())
            if let config=model.config, config.isActive {
                ConsentFormView(model:model,embeddedInForm:true)
                Button("Gửi consent trước khi đăng ký") { submit() }
                    .disabled(busy || !model.hasAllRequired || model.isLoading)
            } else if model.isLoading { ProgressView("Đang tải") }
            else { Text(model.errorMessage ?? "Collection Point chưa sẵn sàng"); Button("Thử lại") { Task { await model.load() } } }
            Button("Mở consent độc lập") { standalone=true }.disabled(busy)
            Button("Đăng xuất") { cmp.clear(); message="Đã xóa quyết định"; Task { await model.load() } }.disabled(busy)
            Text(message)
        }.padding() }
        .onAppear { Task { await model.load() } }
        .sheet(isPresented:$standalone) { StandaloneScreen(cmp:cmp,values:["Email":email]) { message=$0 } }
    }
    private func submit() {
        guard !busy, model.config?.isActive == true, model.validateRequired() else { return }
        busy=true
        Task { @MainActor in
            defer { busy=false }
            do { var state=model.currentState; let sent=try await cmp.submit(&state,values:["Email":email]); message="Đã gửi consentId=\(sent.consentId ?? state.consentId ?? ""). Tiếp tục nghiệp vụ tại đây." }
            catch { message=error.localizedDescription }
        }
    }
}
struct StandaloneScreen: UIViewControllerRepresentable {
    let cmp: ConsentCmp
    let values: [String:String]
    let completed: (String)->Void
    func makeUIViewController(context: Context)->ConsentViewController {
        ConsentViewController(cmp:cmp,values:values,onError:{ completed($0.message) },onCompleted:{ state,result in completed("Đã gửi consentId=\(result.consentId ?? state.consentId ?? "")") })
    }
    func updateUIViewController(_ controller:ConsentViewController,context:Context) {}
}
