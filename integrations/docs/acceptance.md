# Kiểm thử tích hợp

| Tình huống | Kết quả cần có |
| --- | --- |
| Thiếu key hoặc URL sai | Báo lỗi cấu hình, không tiếp tục nghiệp vụ |
| Config đang tải hoặc inactive | Không cho gửi từ nút đăng ký |
| Load 503, mất mạng | Có lỗi hoặc fallback theo SDK, có thể tải lại |
| Mục bắt buộc | Không gửi quyết định thiếu mục bắt buộc |
| Mục tùy chọn tắt | Payload ghi từ chối đúng mục và trường |
| Email, PhoneNumber | Giá trị ánh xạ theo tên và dataType trên config |
| Submit 503 | Giữ màn hình và dữ liệu, người dùng chủ động gửi lại |
| Bấm gửi lặp | Không treo promise, chỉ một lần gửi đang chạy |
| Đóng khi đang gửi | Callback disposed, không suy ra đã hủy request máy chủ |
| Mở lại hoặc đăng xuất | Đọc trạng thái đúng, không giữ giá trị người trước |
| Màn 360dp và chữ 130 phần trăm | Không cắt nội dung; cập nhật chiều cao khi mở mục |
| Debug và release | Đúng origin và artifact, có bản ghi xác nhận |

## Fixture

Chạy `node tools/fixture-server.mjs`, `adb reverse tcp:8099 tcp:8099`.
Android dùng origin `http://127.0.0.1:8099`, key `cp_demo::t_demo`.
Key `fail-load`, `inactive`, `fail-submit` lần lượt kiểm tra lỗi tải, inactive,
lỗi gửi. Chỉ bản debug cho HTTP. GET /records trả bản ghi trong RAM để đối chiếu.
Fixture không gọi API production, không gửi email hoặc tạo tài khoản.

Nghiệm thu UAT thật cần Collection Point và dữ liệu thử đã được chủ hệ thống cho
phép, đối chiếu consentId trong portal. Không coi fixture là chứng nhận UAT.
