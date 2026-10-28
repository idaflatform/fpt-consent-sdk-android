# Kiểm thử Android: UI và phiên bản host

Ngày chốt phiên bản: 16/10/2026. iOS được tạm hoãn theo yêu cầu, ưu tiên Android.

## UI của SDK

Các adapter Android gọi cùng `ConsentFormView` trong SDK 1.2.0 đã đóng gói.
Không có bộ form React/Flutter riêng thay thế giao diện native. Đối chiếu thay
đổi với HEAD của repository Android cho thấy thiết kế, màu và các thành phần
được giữ; typography, khoảng cách và phép đo chiều cao được sửa cho màn hình hẹp.
Vì vậy không cam kết giống từng pixel với bản cũ. Chưa đối chiếu trực quan với
APK Store hoặc mọi artifact SDK đã phát hành trước đây.

Ở chiều rộng form dưới 360dp, form dùng chữ/spacing compact và cho phép xuống
dòng. Kích thước vật lý 720 × 1600 chưa đủ để suy ra dp; còn phụ thuộc density,
padding của ứng dụng và font scale. Kiểm thử layout hiện có dùng density 2,
padding host 24dp mỗi bên, font scale 1.0 và 1.3.

Native Java/XML, Compose và Flutter có ví dụ nhúng form trực tiếp. React Native
và Expo hiện mở modal native; màn bao quanh, ô đăng ký và nút gửi của ví dụ có
thể khác Biznext. Đây không phải component inline trong React/Fabric.

## Các mốc kiểm thử

| Host | Phiên bản được ghim | Trạng thái |
| --- | --- | --- |
| React Native cũ | 0.76.9 / React 18.3.1 | Đang chuẩn bị build host Android |
| React Native mới | 0.88.0 / React 19.3.0 | Đang chuẩn bị build host Android |
| Expo cũ | SDK 52.0.49 / RN 0.76.9 | Đang chuẩn bị prebuild/build Android |
| Expo mới | SDK 58.0.11 / RN 0.88.0 | Đang chuẩn bị prebuild/build Android |

Phiên bản stable được lấy từ npm dist-tags; không dùng nightly/RC/canary.
Đây là các mốc cụ thể, không phải xác nhận mọi phiên bản nằm giữa đều tương thích.
RN 0.76 còn có lựa chọn kiến trúc cũ/mới; host mới dùng kiến trúc mặc định của
template tương ứng. Build đạt cũng chưa thay thế kiểm thử tương tác trên thiết bị.

Nguồn: https://registry.npmjs.org/react-native,
https://registry.npmjs.org/expo,
https://docs.expo.dev/versions/latest/,
https://expo.dev/changelog/2024-11-12-sdk-52,
https://expo.dev/changelog/sdk-58.

## Chạy host

Ví dụ nằm trong `examples/react-native-old`, `react-native-latest`, `expo-old`
và `expo-latest`. Cài dependency bằng `npm ci` khi có lockfile, đặt hai biến môi
trường `CMP_MAVEN_REPO` và `CMP_BRIDGE_MAVEN_REPO` tới repo SDK/bridge đã đóng gói.
RN: chạy wrapper Gradle trong thư mục Android; Expo: chạy `npm run prebuild`
trước rồi build Android. Plugin Expo thêm repo Maven khi prebuild, không chứa key.

Dùng fixture `node tools/fixture-server.mjs` ở root để thử dữ liệu giả. Với thiết
bị USB: `adb reverse tcp:8099 tcp:8099`, nhập `cp_demo::t_demo` và origin
`http://127.0.0.1:8099`. Chỉ dùng HTTP trong build thử nghiệm cho phép cleartext.
Không nhập key production vào source hoặc lockfile.

ADB chưa thấy Sunmi tại thời điểm bắt đầu đợt này. Chưa xác nhận runtime trên
thiết bị cho các host RN/Expo mới.
