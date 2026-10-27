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

```powershell
. D:/Tools/env.ps1
./gradlew.bat :consent-cmp:testDebugUnitTest :consent-cmp:lintRelease :consent-cmp:publishAllPublicationsToLocalRepoRepository --console=plain --max-workers=2
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
