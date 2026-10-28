# Phát hành Android SDK lên Maven Central

Tọa độ bản chuẩn bị phát hành: `io.github.idaflatform:fpt-consent-sdk:1.2.0`.
Chỉ dùng dependency này sau khi Maven Central đã công bố artifact.

## Cấu hình riêng trên máy

Tài khoản Central Portal cần có quyền publish namespace `io.github.idaflatform`.
Tạo user token tại Central Portal. Token gồm username và password riêng cho publishing.

Đặt các property sau trong `$GRADLE_USER_HOME/gradle.properties` (trên máy build
hiện tại là `D:/Tools/.gradle/gradle.properties`), ngoài Git repository:

```properties
mavenCentralUsername=<username của user token>
mavenCentralPassword=<password của user token>
signingInMemoryKey=<toàn bộ private key ASCII armor, gồm BEGIN/END; thay xuống dòng bằng \n>
signingInMemoryKeyPassword=<passphrase nếu khóa được bảo vệ>
```

Có thể dùng biến môi trường `ORG_GRADLE_PROJECT_<tên property>` thay cho file.
Public key tương ứng phải được đưa lên keyserver để Central xác minh chữ ký.
Giữ lại khóa ký và token ở nơi an toàn; không đưa vào source, log hoặc chat.

## Kiểm tra và publish

Chạy từ thư mục gốc repository sau khi cấu hình Java 17 và Android SDK:

### Cách nhanh — script (sau khi làm xong B1)
Từ nhánh `main` đã merge PR release, trong terminal thật:
```bash
scripts/publish-maven-central.sh --dry-run   # kiểm tra + build có ký, KHÔNG upload
scripts/publish-maven-central.sh             # B2 → B5 tự động
```
Script tự làm B2–B5 và dừng ngay khi có điều kiện sai:
- Đang ở `main`, không có thay đổi chưa commit, trùng `origin/main`.
- `sdkVersion` dạng `X.Y.Z`; tag chưa có; `CHANGELOG.md` có mục version và không còn `unreleased`;
  version chưa có trên Central.
- JDK 17/21 và Android SDK.
- Lấy token và khoá theo thứ tự: biến `ORG_GRADLE_PROJECT_*` → `~/.gradle/gradle.properties` → hỏi trên
  terminal (không hiện ký tự). Khoá artifact xuất từ gpg theo `SIGNING_KEY_ID`
  (mặc định `0691C1425EE0A7AF63A39184B6212CBC626C1671`).
- Đủ 5 artifact, 5 chữ ký `.asc` hợp lệ.
- Bắt gõ lại version trước khi upload, rồi upload + release, gắn tag có chữ ký, push tag, chờ tối đa
  45 phút đến khi file tải được từ `repo.maven.apache.org`.

Các bước tay bên dưới giữ để hiểu script làm gì và để xử lý khi script dừng.

### B1. Chuẩn bị version trên nhánh riêng
```bash
git switch main && git pull
git switch -c release/1.2.0
```
Sửa:
- `gradle.properties`: `sdkVersion=1.2.0`
- `README.md`: version trong khối dependency
- `CHANGELOG.md`: đổi `## [1.2.0] - unreleased` thành ngày phát hành

```bash
git commit -am "chore(release): 1.2.0"
git push -u origin release/1.2.0
```
Mở PR vào `main`, chờ review, merge.

### B2. Build và kiểm tra artifact trên máy
Cần Java 17+ (đã kiểm tra với JDK 21) và Android SDK (`ANDROID_HOME`).
```bash
git switch main && git pull
./gradlew :fpt-cmp:testDebugUnitTest :fpt-cmp:lintRelease \
          :fpt-cmp:publishAllPublicationsToLocalRepoRepository --console=plain
ls fpt-cmp/build/repo/io/github/idaplatform/fpt-cmp-sdk/1.2.0/
```
Phải có đủ, **mỗi file kèm `.asc`**:
```
fpt-cmp-sdk-1.2.0.aar            fpt-cmp-sdk-1.2.0.aar.asc
fpt-cmp-sdk-1.2.0.pom            fpt-cmp-sdk-1.2.0.pom.asc
fpt-cmp-sdk-1.2.0.module         fpt-cmp-sdk-1.2.0.module.asc
fpt-cmp-sdk-1.2.0-sources.jar    fpt-cmp-sdk-1.2.0-sources.jar.asc
fpt-cmp-sdk-1.2.0-javadoc.jar    fpt-cmp-sdk-1.2.0-javadoc.jar.asc
```
Không có `.asc` → Gradle đã in cảnh báo `[fpt-cmp] Chua co khoa GPG -> BO QUA ky artifact`. Quay lại A5.

Kiểm tra chữ ký một file:
```bash
gpg --verify fpt-cmp/build/repo/io/github/idaplatform/fpt-cmp-sdk/1.2.0/fpt-cmp-sdk-1.2.0.pom.asc
```

Kiểm tra thư mục `consent-cmp/build/repo/io/github/idaflatform/fpt-consent-sdk/1.2.0`:
AAR, POM, sources JAR và Javadoc JAR đều cần có chữ ký `.asc`.

Sau khi xác nhận artifact và chữ ký, upload và tự động release:

```powershell
./gradlew.bat :consent-cmp:publishAndReleaseToMavenCentral --console=plain --max-workers=2
```

Xem trạng thái tại <https://central.sonatype.com/publishing/deployments>.
Chỉ thông báo phát hành thành công khi deployment được publish và POM/AAR có thể tải tại:
<https://repo.maven.apache.org/maven2/io/github/idaflatform/fpt-consent-sdk/1.2.0/>.
Phiên bản đã publish không thể ghi đè.

Tham khảo: [Sonatype requirements](https://central.sonatype.org/publish/requirements/),
[Gradle Maven Publish](https://vanniktech.github.io/gradle-maven-publish-plugin/central/).
