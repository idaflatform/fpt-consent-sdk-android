# MAUI native modal integration

Example là ứng dụng MAUI Android/iOS tối thiểu với hai chế độ native modal.
AndroidBinding tạo .NET binding từ mobile-bridge AAR; SDK AAR là dependency không
bind. iOSBinding chỉ bind Objective C facade, không cố bind trực tiếp Swift
struct/generic API.

Build Android với .NET 9 + workload MAUI/Android và các thuộc tính
`CmpBridgeAar`, `CmpSdkAar` trỏ tới release artifact. Build iOS trên Mac cần thêm
`CmpBridgeXCFramework`, `CmpSdkXCFramework` và cấu hình ký của host.

Cần chạy binding generator, kiểm tra tên callback được sinh ra và dependency
Material/AndroidX trong môi trường MAUI trước khi phát hành. Chưa build hoặc
xác nhận binding trên PC này. Không có inline MAUI handler trong bản nguồn này.
