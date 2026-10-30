# CMP Capacitor

Cài package vào host Capacitor, chạy `npx cap sync`, dùng web bundler để build
example/index.html rồi build Android/iOS. Native dependency là mobile-bridge
0.1.0 (Maven) và CMPMobileBridge 0.1.0 (CocoaPods). Xem hướng dẫn artifact cục bộ
ở README gốc. Cả hai chế độ hiển thị một màn native riêng; chưa có native view
nhúng trong DOM. Không dùng package này trên web thuần.

Mã nguồn adapter hiện chờ build trên host Capacitor và kiểm thử thiết bị trước
khi công bố phiên bản hỗ trợ. Swift implementation dùng CAPBridgedPlugin.

Android adapter dùng AGP 9.5.0. Host cần Gradle 9.6.0 trở lên và AGP phù hợp;
không ghép plugin này vào host vẫn pin AGP 8.x / Gradle 8.x. Build dependency
này thuộc ứng dụng host, không được thay đổi chỉ bằng `npx cap sync`.
