# Kết quả xác minh

Ngày 15 tháng 10 năm 2026. Chỉ các phép kiểm tra liệt kê dưới đây đã được thực
hiện. Có mã nguồn hoặc file workflow không đồng nghĩa framework đã được hỗ trợ
production hoặc các mục WBS đã nghiệm thu.

## Đã xác minh

* SDK Android 1.2.0 được đóng gói thành Maven artifact tại repository cục bộ.
* Java native và Kotlin Compose đã build debug và release tối ưu R8.
* Native bridge đã build AAR release và publish vào Maven repository cục bộ.
* Bốn bài Robolectric đạt: thiếu Activity, URL sai, busy/đóng dialog một lần,
  và SDK thật GET config/POST consent tới HTTP loopback với ánh xạ email và
  consentId khớp bản ghi fixture.
* Máy chủ fixture Node đạt kiểm tra config, validate và ghi payload thành công,
  cùng lỗi tải/gửi mô phỏng. Không gọi backend thật hoặc gửi email.
* Bốn bài kiểm thử Dart đạt, gồm cấu hình, form chưa gắn view và khôi phục chiều
  cao native ban đầu với làm tròn lên để tránh cắt nội dung.
* Bài kiểm thử ứng dụng Flutter xác nhận không mở đăng ký trước khi cấu hình đạt.
* Flutter analyze cuối cùng không còn lỗi hoặc cảnh báo.
* Phần Kotlin native của plugin Flutter compile thành công.
* Flutter example build APK debug và release; bản release khoảng 28 MB, kiểm tra
  chữ ký đạt, không có debug flag và có tài nguyên compact của SDK đã sửa.
  Chữ ký của example là khóa thử nghiệm, không phải khóa phát hành Store.
* Ứng dụng KMP Android build debug thành công với artifact bridge đã đóng gói.

Các phép kiểm tra trên chạy cục bộ, chưa phải lần chạy CI hoặc nghiệm thu thiết bị.

## Chờ xác minh

* iOS: PC Windows không có Xcode, simulator hoặc iPhone. SwiftUI, UIKit,
  Objective C, Flutter iOS và các facade Swift chưa được compile/runtime test.
* React Native, Capacitor, Cordova chưa có host native build được xác minh.
  Ví dụ React Native/web là entry point để lắp vào host framework phù hợp.
* React Native/Fabric inline component chưa triển khai; adapter hiện tại chỉ
  mở modal. Đã thêm config plugin Expo và host RN/Expo cũ/mới; chưa xác minh
  prebuild/native build/runtime, chỉ cài dependency thành công.
* MAUI chưa có workload Android/iOS trong .NET cài trên PC; binding generator,
  tên API sinh ra, NuGet dependency và app chưa được build xác nhận.
* Unity chưa có lần build Editor/IL2CPP hoặc kiểm thử thiết bị trong tác vụ này.
* KMP iOS, Swift XCFramework và liên kết framework cần Mac/Xcode.
* Sunmi không còn xuất hiện trong ADB tại thời điểm kiểm thử; không khẳng định
  đã chạy các ứng dụng mẫu mới trên Sunmi.

## Điều kiện trước khi phát hành

1. Hoàn tất build/test từng host và các luồng theo docs/acceptance.md.
2. Hoàn thiện React Native inline/Fabric nếu giữ phạm vi này trong WBS.
3. Đối chiếu consentId trong portal UAT thật với dữ liệu thử được phép sử dụng.
4. Lập trình viên độc lập tích hợp theo tài liệu; xác nhận ma trận phiên bản.
5. Chốt license, phiên bản, cấu hình ký và repository phân phối. Chưa publish
   Maven Central, CocoaPods, pub.dev, npm hoặc NuGet trong tác vụ này.
6. Kích hoạt và chạy CI trong repository thực; không coi workflow cục bộ là CI đã đạt.

WBS chưa được đánh dấu hoàn tất toàn bộ; các mục yêu cầu iPhone, host framework,
Fabric, kiểm chứng portal và phát hành vẫn còn mở.

## Bổ sung kiểm thử SUNMI — 16/10/2026

Sau khi kết nối lại USB, ADB nhận SUNMI V2 Pro VB02205F01044. Đã cài bằng
`adb install -r` và mở Flutter example release (package
`vn.fpt.cmp.cmp_consent_example`, APK release đã build trước đó). Màn hình thực
720 × 1440, density 320; không ghi nhận đây là thiết bị 720 × 1600.

Form nhúng tải từ fixture trên PC qua ADB reverse cổng 8099. Đã mở rộng hai
mục đích; nội dung, trường Email và nút gửi hiển thị đầy đủ. Gửi dữ liệu giả
`demo@example.invalid` thành công: callback trong app và record fixture có cùng
consentId `1bdb6a1f-29cf-421a-a09f-64b164dbc601`. Record xác nhận registration
được chấp nhận, marketing không được chấp nhận. Không gọi backend UAT/production
hoặc gửi email trong phép thử này. Chưa suy ra runtime RN/Expo từ kết quả Flutter.

Bằng chứng trong `../artifacts/sdk-integration-plan/`:
`sunmi-release-expanded.png`, `sunmi-release-submit.png`,
`sunmi-fixture-records.json`. Google Play trên thiết bị có hộp thoại crash lặp lại;
đã force-stop riêng package Google Play để tiếp tục kiểm thử ứng dụng mẫu.
