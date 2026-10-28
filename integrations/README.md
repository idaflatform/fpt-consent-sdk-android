# Tích hợp CMP cho ứng dụng Android và iOS

Bộ tích hợp tách khỏi Biznext. Không chứa key khách hàng. SDK lõi Android/iOS
được giữ tại repository hiện có; thư mục `integrations/` trong repository Android
chứa adapter và ứng dụng mẫu cho các nền tảng.

## Nội dung

| Thư mục | Nội dung |
| --- | --- |
| android/java-example | Java với layout XML, consent nhúng và bottom sheet |
| android/compose-example | Kotlin Compose với AndroidView và bottom sheet |
| android/mobile-bridge | Facade JSON cho màn native độc lập hoặc đăng ký native |
| ios/Examples | Ví dụ SwiftUI, UIKit, Objective C; tạo project bằng XcodeGen |
| ios/Bridge | Objective C facade từ SDK Swift cho các binding |
| packages/flutter | Plugin và host Android/iOS; nhúng native view trực tiếp |
| packages/react-native | Module modal và App.tsx mẫu; chưa có inline Fabric component |
| packages/capacitor | Native modal plugin và web entry mẫu |
| packages/cordova | Plugin tùy chọn và web entry mẫu |
| packages/kmp | Giao tiếp commonMain và host Android/iOS |
| packages/maui | Binding projects và ứng dụng mẫu Android/iOS |
| packages/unity | Plugin JNI/iOS và sample UI dựng từ script |

## SDK và phiên bản

Android dùng Maven `io.github.idaplatform:fpt-cmp-sdk:1.2.0`. Ví dụ không
includeBuild SDK. Bản 1.2.0 đã đóng gói cục bộ không đồng nghĩa đã lên Maven
Central. iOS Swift Package trỏ từ 1.1.0; podspec ConsentCmp 1.2.0 mới được bổ sung
ở repository iOS để chuẩn bị CocoaPods. Pod/plugin chưa được phát hành công khai.

## Build native Android

Gói ZIP bàn giao có `vendor/maven/sdk`, `vendor/maven/bridge` và snapshot nguồn
iOS `vendor/ConsentCmp`. Sau giải nén, đặt CMP_MAVEN_REPO, CMP_BRIDGE_MAVEN_REPO
và CMP_IOS_SDK_PATH tới các thư mục này. Đây là artifact/snapshot kiểm thử cục bộ,
không phải xác nhận đã được phát hành trên registry công khai.

Checkout Git chỉ chứa source. Build SDK lõi ở root repository bằng
`./gradlew :fpt-cmp:publishAllPublicationsToLocalRepoRepository`, rồi đặt
`CMP_MAVEN_REPO` tới `fpt-cmp/build/repo` bằng đường dẫn tuyệt đối. Build
bridge từ `integrations/android` như dưới đây và đặt `CMP_BRIDGE_MAVEN_REPO` tới
repo bridge tương ứng. Checkout repository iOS cạnh repository Android hoặc
truyền đường dẫn riêng qua `CMP_IOS_SDK_PATH`.

Script `python tools/package-source.py --help` hỗ trợ đóng gói ZIP từ các checkout
và artifact local. `tools/configure-generated.py` là bootstrap một lần, có ghi đè
file host; không chạy script này khi build các host đã có trong Git.

JDK 17, Android SDK 35 và Gradle wrapper đã có trong android.

```sh
cd android
./gradlew :java-example:assembleDebug :compose-example:assembleDebug \
  -PcmpCodeConfig=cp_xxx::t_yyy -PcmpBaseUrl=https://cmp.example.com
```

Artifact nội bộ: `-PcmpMavenRepo=/path/to/maven-repo` hoặc CMP_MAVEN_REPO.
Native bridge: `:mobile-bridge:publishReleasePublicationToIntegrationRepository`;
repository bridge được tạo trong android/build/repo. Adapter khác đọc
CMP_BRIDGE_MAVEN_REPO. AAR mobile-bridge không gộp dependency của SDK; host phải
resolve đầy đủ Maven dependency. Release native examples chưa ký cho phân phối.

## iOS

Mac, Xcode, XcodeGen; chạy `xcodegen generate` tại ios/Examples rồi mở project
CMPExamples.xcodeproj, chọn scheme. Cấu hình key và URL trong giao diện ví dụ.
Flutter iOS dùng CocoaPods và pod ConsentCmp. Kiểm thử trước khi publish pod:
đặt CMP_IOS_SDK_PATH đến checkout SDK iOS chứa ConsentCmp.podspec.
Frameworks cho MAUI/Unity phải được build/link/embed trên Mac; không có binary
iOS tạo từ Windows và không khẳng định đã kiểm thử trên iPhone.
Script `tools/build-xcframeworks.sh` dùng XcodeGen, Xcode và CMP_IOS_SDK_PATH để
tạo hai XCFramework chưa ký. Script và dự án framework cần được xác nhận trên Mac.

## Fixture và kiểm thử

```sh
node --test tools/fixture-server.test.mjs
node tools/fixture-server.mjs
adb reverse tcp:8099 tcp:8099
```

Origin debug `http://127.0.0.1:8099`, key `cp_demo::t_demo`. Xem
docs/acceptance.md và docs/integration-contract.md. Không dùng HTTP fixture trong
release. Không tự gọi backend production, gửi email hoặc tạo tài khoản mẫu.

## Trước khi phát hành

Trạng thái từng mã WBS được ghi trong docs/wbs-status.md.

File .github/workflows/verify.yml trong thư mục integrations là mẫu workflow,
không tự chạy từ thư mục lồng. Khi đưa vào .github/workflows ở root repository,
cần đặt working-directory là integrations. Workflow cần artifact SDK truy cập được và pod iOS
đã public hoặc source nội bộ cấu hình riêng. Không đánh dấu pipeline đã chạy
chỉ vì file workflow tồn tại. Xem validation.md để biết kiểm thử thực tế và phần
còn lại; các adapter modal không thay thế inline view theo WBS.
