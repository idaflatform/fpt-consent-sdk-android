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

## Dependency lockfiles

`AndroidBinding/packages.lock.json` và `IOSBinding/packages.lock.json` được tạo
bằng SDK .NET 9.0.320 (pin trong `global.json`) sau khi cài workload
`android` / `ios` với `dotnet workload install android ios --skip-manifest-update`.
iOS binding chưa có `PackageReference` riêng; lockfile vẫn ghi nhận
`Microsoft.NET.ILLink.Tasks` do SDK thêm vào.

```sh
dotnet restore AndroidBinding/AndroidBinding.csproj --locked-mode
dotnet restore IOSBinding/IOSBinding.csproj --locked-mode
```

Khi chủ động thay dependency, chạy `dotnet restore --force-evaluate` cho project
đã đổi, review và commit lockfile cùng `.csproj`. Build iOS vẫn cần Mac/Xcode.
Scanner phải đọc lockfile ở thư mục con, gồm cả dependency bắc cầu của
`Xamarin.Google.Android.Material`; không chỉ quét project ứng dụng Example.
