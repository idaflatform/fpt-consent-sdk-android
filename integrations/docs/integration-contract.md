# Giao tiếp tích hợp CMP

## Cấu hình

`codeConfig` là integration key của Collection Point. `baseUrl` là origin CMP,
không kèm API path. Ví dụ phải nhập cấu hình hoặc dùng fixture, không đóng gói key
khách hàng. Không tự bỏ qua consent khi cấu hình thiếu hoặc tải thất bại.

## Form nhúng

1. Khởi tạo SDK trước khi tạo form.
2. Tải config, kiểm tra active và thông báo ready.
3. Truyền `values` dạng map tên trường hoặc `name|dataType` sang chuỗi.
4. Chỉ gửi khi ready, active, đủ mục bắt buộc và chưa gửi.
5. Chờ kết quả trước khi tiếp tục nghiệp vụ. `consentId` là bằng chứng gửi thành công.

Không suy ra đồng ý từ việc đóng màn hình. Khi hủy, trả lỗi `cancelled`.
Các lỗi chuẩn: `not_configured`, `not_ready`, `inactive`, `busy`,
`submit_failed`, `disposed`, `cancelled`, `invalid_arguments`.
Lỗi mạng có `httpStatus=0`. Không tự retry POST: người dùng chủ động thử lại.
Mỗi callback/promise chỉ được hoàn tất một lần, kể cả khi view bị dispose.

## Trạng thái

`loading`, `ready`, `failed`, `inactive`, `disposed`. Trạng thái ready có
`allRequiredGranted`. View cung cấp status hiện tại để tránh mất sự kiện đầu tiên.
Framework quản lý chiều cao phải cập nhật khi tải, đổi lựa chọn hoặc mở mục đích.

## Tài khoản và vòng đời

`clear` xóa quyết định, cache và nguồn giá trị khi đăng xuất; visitorId giữ nguyên
theo SDK. Không gọi clear hoặc configure khi còn form/screen đang hoạt động.
SDK không tự bật/tắt analytics hoặc tính năng app; host phải dùng isGranted.

## Nghiệm thu

Hai luồng: màn consent độc lập và form đăng ký nhúng. Kiểm tra debug/release,
chữ lớn, chiều rộng nhỏ, mở mục đích, lỗi tải/gửi, bấm lặp, hủy và mở lại.
Fixture chỉ chứng minh payload và callback; nghiệm thu CMP thật cần đối chiếu portal.
