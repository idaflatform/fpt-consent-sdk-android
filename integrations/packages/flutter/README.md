# CMP Consent Flutter

Plugin Android/iOS với native form, không phụ thuộc Biznext. API công khai trong
`lib/cmp_consent.dart`: configure, CmpConsentForm, ConsentFormController.submit,
reload, clear, isGranted và savedConsentId. Ví dụ có màn độc lập và form đăng ký.

## Chạy ví dụ

Sau khi SDK Android 1.2.0 đã có trong Maven repository:

```sh
cd example
flutter pub get
flutter run --dart-define=CMP_CODE_CONFIG=cp_xxx::t_yyy --dart-define=CMP_BASE_URL=https://cmp.example.com
```

Thử artifact chưa phát hành: đặt biến môi trường `CMP_MAVEN_REPO` tới repository
chứa artifact đã đóng gói. Không dùng includeBuild/source substitution.

iOS cần Xcode, CocoaPods và ConsentCmp 1.2.x. Podspec SDK đi kèm kho SDK iOS;
trước khi phát hành lên CocoaPods, đặt `CMP_IOS_SDK_PATH` tới checkout SDK có
ConsentCmp.podspec rồi chạy `pod install` trong example/ios. Đây là đường kiểm thử
nguồn iOS trước phát hành, chưa phải chứng nhận package đã public.

## Luồng ứng dụng

Configure trước khi tạo form, giữ controller ổn định trong vòng đời view.
Màn đăng ký dùng `embedded:true`, truyền giá trị mới nhất khi gọi submit và chờ
consentId trước khi tiếp tục. `embedded:false` hiện nút gửi của SDK, nhận
onSubmitted và truyền values vào widget. Mỗi engine dùng một form tại một thời điểm.
Không configure khi form còn tồn tại. clear chỉ thực hiện khi không đang gửi.

Các sự kiện tải và chiều cao được phục hồi bằng status sau khi gắn channel.
Dispose trả lỗi cho submit còn chờ; request đã gửi lên server không thể thu hồi.
Không tự retry POST. Xem ../../docs/integration-contract.md từ gốc bộ tích hợp.

Plugin hiện là release candidate cục bộ, chưa phát hành pub.dev. Tình trạng build
và thiết bị được ghi riêng trong validation.md tại gốc bộ tích hợp.

## Dependency locking and Android build tools

Commit `pubspec.lock` của plugin để security scanner có đầy đủ phiên bản và
checksum đã resolve. CI dùng `flutter pub get --enforce-lockfile`; cập nhật
lockfile có chủ đích bằng `flutter pub get` rồi review diff.

Android example dùng AGP 9.5.0 / Gradle 9.6.0 và Kotlin 2.4.21. Với Flutter
Gradle integration còn dùng legacy Android DSL, host cần hai property
`android.builtInKotlin=false` và `android.newDsl=false` như example. Các property
này thuộc host; plugin không tự ghi đè cấu hình build của ứng dụng tích hợp.
