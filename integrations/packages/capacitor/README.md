# CMP Capacitor

Cài package vào host Capacitor, chạy `npx cap sync`, dùng web bundler để build
example/index.html rồi build Android/iOS. Native dependency là mobile-bridge
0.1.0 (Maven) và CMPMobileBridge 0.1.0 (CocoaPods). Xem hướng dẫn artifact cục bộ
ở README gốc. Cả hai chế độ hiển thị một màn native riêng; chưa có native view
nhúng trong DOM. Không dùng package này trên web thuần.

Mã nguồn adapter hiện chờ build trên host Capacitor và kiểm thử thiết bị trước
khi công bố phiên bản hỗ trợ. Swift implementation dùng CAPBridgedPlugin.
