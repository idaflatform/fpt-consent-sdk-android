# CMP React Native

Adapter modal native và ứng dụng mẫu trong example/App.tsx. Hai chế độ: consent
độc lập hoặc màn đăng ký native chứa form consent và nút của host. Chưa có native
component nhúng trực tiếp trong cây React/Fabric. Không quảng bá là hỗ trợ inline.

Cài package qua đường dẫn hoặc archive npm, rebuild native app để autolink.
SDK Android và mobile-bridge cần có trong Maven repository; biến CMP_MAVEN_REPO
và CMP_BRIDGE_MAVEN_REPO dùng kiểm thử artifact cục bộ. iOS Podfile cần local pod
ConsentCmp và CMPMobileBridge trước khi các pod được phát hành.

Ví dụ host: tạo ứng dụng React Native bằng CLI chính thức, cài package này,
thay App.tsx bằng ví dụ. Dùng đúng React Native version của host, không tải ngầm
phiên bản mới trong quá trình tích hợp.

Expo: cần development build/prebuild, thêm native dependency rồi build Android/iOS.
Expo Go không chứa module này. Chưa có Expo config plugin hoặc xác nhận Fabric;
kiểm thử các mục này là điều kiện phát hành, không suy ra từ việc có mã nguồn.
