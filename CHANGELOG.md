# Changelog

Định dạng theo [Keep a Changelog](https://keepachangelog.com/vi/1.1.0/), version theo [SemVer](https://semver.org/).

## [Unreleased]

### Security
- Commit Flutter and MAUI binding lockfiles for dependency/security discovery.
- Publish Kotlin stdlib/BOM 2.4.21 instead of relying on AndroidX's older runtime.
- Upgrade release/integration builds to AGP 9.5.0, Gradle 9.6.0 and publishing plugin 0.37.0.
- Upgrade Robolectric to 4.17; its previous test graph also contained Bouncy Castle 1.78.1.
- Add SDK backup-exclusion resources and host integration guidance without changing the customer's backup policy.

### Build compatibility
- Migrate the KMP Android target to the Android KMP library plugin.
- Kotlin hosts need a compiler compatible with Kotlin 2.4 metadata; Flutter's legacy host uses explicit AGP compatibility properties.

## [1.2.1] - 2026-10-29

### Changed
- Automatically apply changed appearance from each fresh consent config response without an additional API request.
- Keep the last valid appearance as offline/invalid-response fallback; reuse unchanged appearance and prepared fonts.
- Remove appearance refresh and cache-clear buttons from the sample app.

### Fixed
- Recover from initially missing appearance when a later config response is valid in the same session.
- Prevent older cached consent content from downgrading a newer theme.
- Compare appearance independently of JSON object key order and detect changed tokens even when revision is unchanged.

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
