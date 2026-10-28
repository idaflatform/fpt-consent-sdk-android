# Changelog

Định dạng theo [Keep a Changelog](https://keepachangelog.com/vi/1.1.0/), version theo [SemVer](https://semver.org/).

## [1.2.0] - 2026-10-28

### Changed — breaking cho cách khai dependency
- Đổi tọa độ Maven: `io.github.idaflatform:fpt-consent-sdk` → **`io.github.idaplatform:fpt-cmp-sdk`**.
  ```diff
  - implementation("io.github.idaflatform:fpt-consent-sdk:1.1.0")
  + implementation("io.github.idaplatform:fpt-cmp-sdk:1.2.0")
  ```
  Package Java (`vn.fpt.fis.cmp.consent`) không đổi — code app không cần sửa import.
- Module Gradle trong repo đổi tên `consent-cmp` → `fpt-cmp` (task: `:fpt-cmp:...`).
- Org GitHub đổi tên `idaflatform` → `idaplatform`.

### Added
- Giao diện consent accordion, app mẫu (`sample`), adapter tích hợp mobile (`integrations/`).
- Cache appearance (màu, font) cấu hình từ CMP.

### Fixed
- Crash trên Android 7; đo kích thước form compact trước khi layout.
- Đọc appearance đã cache từ response cấu hình consent.

## [1.1.0], [1.0.2], [1.0.1], [1.0.0]

Phát hành tay lên Maven Central dưới `io.github.idaflatform:fpt-consent-sdk`, **không có tag Git tương ứng**
(lịch sử `gradle.properties` không xác định được commit của từng bản). Các bản này giữ nguyên trên Central
và không được cập nhật tiếp.
