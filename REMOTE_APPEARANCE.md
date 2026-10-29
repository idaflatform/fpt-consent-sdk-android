# Giao diện và font từ CMP Portal

SDK đọc `data.appearance` trong response `GET {baseUrl}{apiPath}/config?code_config=...`,
qua cùng header `X-Consent-Integration` của nội dung consent. Appearance gồm
`schemaVersion=1`, `theme`, `typography` và `revision`. Khi tải form, SDK dùng chung
response config, không gửi thêm request để lấy giao diện.

Mỗi lần tải form thành công từ API, SDK tự áp dụng và lưu appearance hợp lệ mới
ngay từ cùng response `/config`; không cần thao tác của người dùng hoặc request
riêng. Appearance không đổi được dùng lại, không ghi cache hay tải lại font.
Cache riêng theo base URL, API path và integration key, không TTL, là bản dự phòng
khi offline hoặc response thiếu/sai appearance. Nếu chưa có bản tốt, SDK dùng mặc
định; response hợp lệ ở lần tải kế tiếp vẫn được áp dụng trong cùng phiên.
Nội dung consent lấy từ cache không ghi đè một appearance mới hơn đã lưu.

Các form tự tải cấu hình áp dụng appearance tự động. App bind config thủ công
có thể gọi `getAppearance(callback)` trước và `form.setAppearance(appearance)`.

```java
ConsentCmp.get().getAppearance(new ConsentCallback<ConsentAppearance>() {
    public void onSuccess(ConsentAppearance appearance) { form.setAppearance(appearance); }
    public void onError(ConsentException error) { /* appearance errors use defaults */ }
});
```

Các API refresh/clear chỉ dành cho tích hợp cần điều khiển cache; app mẫu không
đưa chúng thành nút cho khách hàng. Refresh lỗi giữ bản tốt gần nhất. Callback trả
appearance đang dùng (có thể là bản cũ hoặc mặc định); revision cho biết bản đã nhận. Form đang mở giữ snapshot cho đến
khi app reload/rebind hoặc gọi setAppearance. Clear và get/refresh được xếp hàng trên
executor SDK. `clear()`/logout consent không xóa cache appearance.

Portal: Data Consent → Collection Point → Giao diện SDK. Có thể chọn màu, cỡ chữ,
độ đậm, bo góc và tải tối đa 4 font TTF/OTF (4 MB/file). Font tải từ public HTTPS,
không redirect, không gửi integration key hoặc token của app. SDK kiểm tra SHA-256,
lưu file trong app-private files và dùng lại mà không tải thêm. Font không tải được
hoặc mất file dùng system font; refresh thủ công để thử lại. Font đã đăng ký trong
app có thể được chọn bằng tên family; hỗ trợ tên family tùy nền tảng. Android cũ
ánh xạ weight về regular/bold khi không có file weight tương ứng.

Theme được cache thành công ngay cả khi font tải lỗi. Các font được lưu theo hash
và dùng chung giữa Collection Point; clear cấu hình không xóa file font đang được
profile khác dùng. Clear app data xóa toàn bộ; hành vi restore sau reinstall phụ
thuộc backup của app chủ. Không đưa file font vào OS cache dễ bị thu hồi.

App mẫu tự lấy appearance khi tải form và dùng cache; không có nút cập nhật/xóa
giao diện cho người dùng. Fixture trong `integrations/tools` trả theme tím/font serif;
`/requests` cho phép đếm request config.

Triển khai backend, gateway và portal hỗ trợ `data.appearance` trên `/consent/config`
trước khi rollout SDK. Backend cũ thiếu appearance dùng giao diện mặc định. Việc
này chưa tự deploy hoặc publish artifact lên Maven Central.
