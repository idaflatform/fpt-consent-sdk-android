# Giao diện và font từ CMP Portal

SDK gọi `GET {baseUrl}{apiPath}/appearance?code_config=...` khi chưa có appearance
đã lưu. API gửi cùng header `X-Consent-Integration` như `/config`; backend trả
`data.schemaVersion=1`, `theme`, `typography` và `revision`.

Sau lần tải thành công, SDK dùng cache riêng theo base URL, API path và integration
key. Cache không có TTL; mở lại form/app hoặc cập nhật app không gọi lại endpoint
appearance. `/config` vẫn tải nội dung consent như trước. Nếu lần đầu lỗi thì dùng
giao diện mặc định và thử lại ở phiên SDK tiếp theo.

Các form tự tải cấu hình áp dụng appearance tự động. App bind config thủ công
có thể gọi `getAppearance(callback)` trước và `form.setAppearance(appearance)`.

```java
ConsentCmp.get().getAppearance(new ConsentCallback<ConsentAppearance>() {
    public void onSuccess(ConsentAppearance appearance) { form.setAppearance(appearance); }
    public void onError(ConsentException error) { /* appearance errors use defaults */ }
});
// Khi app chủ động muốn nhận thay đổi mới của portal:
ConsentCmp.get().refreshAppearance(callback);
// Chỉ xóa giao diện; giữ nguyên visitor ID và consent:
ConsentCmp.get().clearAppearanceCache();
```

Refresh lỗi giữ bản tốt gần nhất. Callback trả appearance đang dùng (có thể là bản
cũ hoặc mặc định); revision cho biết bản đã nhận. Form đang mở giữ snapshot cho đến
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

App mẫu có nút cập nhật/xóa appearance. Fixture trong `integrations/tools` trả theme
tím/font serif; `/requests` cho phép đếm riêng request appearance và config.

Triển khai backend migration `V211__collection_point_mobile_appearance.sql`, CMP,
gateway và portal trước khi rollout SDK. SDK mới gặp backend chưa có endpoint dùng
mặc định. Việc này chưa tự deploy hoặc publish artifact lên Maven Central.
