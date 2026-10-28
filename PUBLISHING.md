# Phát hành Android SDK lên Maven Central

Tài liệu cho **maintainer** của repo. Lập trình viên app chỉ cần [README.md](README.md).

| Mục | Giá trị |
|---|---|
| Tọa độ | `io.github.idaplatform:fpt-cmp-sdk:<version>` |
| Module Gradle | `:fpt-cmp` |
| Danh tính commit | `idaplatform <fptcmp@fpt.com>` |
| Khoá ký commit / tag | ed25519 `9461F352FF27F7832AE0872E305CA850DB3AD879` |
| Khoá ký artifact (Central) | rsa4096 `0691C1425EE0A7AF63A39184B6212CBC626C1671` |
| Tag | `X.Y.Z` (không prefix `v`), có chữ ký |

Bản ≤ `1.1.0` nằm ở tọa độ cũ `io.github.idaflatform:fpt-consent-sdk` và không cập nhật nữa.

**Hai khoá, hai việc — không dùng lẫn:** khoá ed25519 chỉ ký commit/tag Git; khoá rsa4096 chỉ ký
artifact gửi lên Central. Lộ một khoá thì chỉ phải thu hồi khoá đó.

---

## Phần A — Chuẩn bị một lần trên máy maintainer

Mọi lệnh có nhập passphrase phải chạy trong **terminal thật** (Terminal.app, iTerm, PowerShell).
Chạy từ IDE, app Git hoặc tool không có TTY sẽ lỗi `Inappropriate ioctl for device`.

### A1. Cho gpg hỏi passphrase được

macOS / Linux:
```bash
echo 'export GPG_TTY=$(tty)' >> ~/.zshrc
source ~/.zshrc
gpgconf --kill gpg-agent
```
Commit từ IDE / app Git trên macOS thì dùng cửa sổ nhập passphrase:
```bash
brew install pinentry-mac
echo "pinentry-program $(brew --prefix)/bin/pinentry-mac" >> ~/.gnupg/gpg-agent.conf
gpgconf --kill gpg-agent
```

### A2. Danh tính commit và chữ ký (chỉ trong repo này)
```bash
git config user.name  idaplatform
git config user.email fptcmp@fpt.com
git config user.signingkey 9461F352FF27F7832AE0872E305CA850DB3AD879
git config commit.gpgsign true
git config tag.gpgsign true
```
Chưa có khoá ký commit trên máy thì tạo (hoặc nhập khoá từ người giữ khoá):
```bash
gpg --quick-gen-key "idaplatform <fptcmp@fpt.com>" ed25519 sign 2y
```

Đưa public key lên GitHub — **tài khoản GitHub sở hữu email `fptcmp@fpt.com`** (email phải ở trạng thái
*Verified* trong Settings → Emails):
```bash
gpg --armor --export 9461F352FF27F7832AE0872E305CA850DB3AD879 | pbcopy
# GitHub → Settings → SSH and GPG keys → New GPG key → dán
```

Kiểm tra:
```bash
echo test | gpg --clearsign --local-user 9461F352FF27F7832AE0872E305CA850DB3AD879 >/dev/null && echo "ky duoc"
git config user.signingkey      # phai ra 9461F352FF27F7832AE0872E305CA850DB3AD879
```
Sau commit đầu tiên: `git log -1 --show-signature` phải thấy
`Good signature from "idaplatform <fptcmp@fpt.com>"` và key `…305CA850DB3AD879`.

### A3. Tài khoản Central Portal và namespace

1. Đăng nhập <https://central.sonatype.com> bằng tài khoản của nhóm.
2. **Namespaces → Add Namespace →** `io.github.idaplatform`. Portal đưa ra một verification key.
3. Tạo repo **public** tạm `idaplatform/<verification-key>` trên GitHub (cần quyền admin org) → bấm
   **Verify** → khi trạng thái là *Verified* thì xoá repo tạm.
4. **Account → Generate User Token** → lưu `username` và `password` của token (chỉ hiện một lần).

Namespace chưa *Verified* thì mọi lần publish đều bị từ chối.

### A4. Khoá ký artifact

Đưa public key lên keyserver để Central xác minh chữ ký (làm một lần, mỗi khi gia hạn khoá thì gửi lại):
```bash
gpg --keyserver keyserver.ubuntu.com --send-keys 0691C1425EE0A7AF63A39184B6212CBC626C1671
gpg --keyserver keys.openpgp.org     --send-keys 0691C1425EE0A7AF63A39184B6212CBC626C1671
```
`keys.openpgp.org` gửi mail xác nhận tới `fptcmp@fpt.com`; phải bấm link thì khoá mới tìm được theo email.

Kiểm tra (HTTP 200 là đã có):
```bash
curl -s -o /dev/null -w "%{http_code}\n" \
  "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x0691C1425EE0A7AF63A39184B6212CBC626C1671"
```

### A5. Cấp bí mật cho Gradle — ngoài Git repository

Không bao giờ ghi các giá trị này vào `gradle.properties` của repo.

**Cách 1 — biến môi trường cho phiên terminal hiện tại (khuyến nghị khi publish tay):**
```bash
export ORG_GRADLE_PROJECT_mavenCentralUsername='<username của user token>'
export ORG_GRADLE_PROJECT_mavenCentralPassword='<password của user token>'
export ORG_GRADLE_PROJECT_signingInMemoryKey="$(gpg --armor --export-secret-keys 0691C1425EE0A7AF63A39184B6212CBC626C1671)"
export ORG_GRADLE_PROJECT_signingInMemoryKeyPassword='<passphrase khoá artifact>'
```
Đóng terminal sau khi publish để xoá các biến này.

**Cách 2 — `~/.gradle/gradle.properties` (file của user, không nằm trong repo):**
```properties
mavenCentralUsername=<username của user token>
mavenCentralPassword=<password của user token>
signingInMemoryKey=<private key ASCII armor, giữ BEGIN/END, thay xuống dòng bằng \n>
signingInMemoryKeyPassword=<passphrase khoá artifact>
```

---

## Phần B — Mỗi lần phát hành

Ví dụ phát hành `1.2.0`.

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

### B3. Upload lên Maven Central
```bash
./gradlew :fpt-cmp:publishAndReleaseToMavenCentral --no-configuration-cache --console=plain
```
- Lệnh upload rồi **tự release** (cấu hình `publishToMavenCentral(CENTRAL_PORTAL, true)` trong
  `fpt-cmp/build.gradle`). Muốn tự bấm Publish trên Portal để có bước duyệt: đổi `true` → `false`, khi đó
  dùng lệnh `publishToMavenCentral` và vào <https://central.sonatype.com/publishing/deployments> bấm **Publish**.
- Theo dõi trạng thái tại <https://central.sonatype.com/publishing/deployments>:
  `VALIDATING → VALIDATED → PUBLISHING → PUBLISHED`. `FAILED` thì mở deployment xem lý do (bảng ở phần D).

### B4. Gắn tag có chữ ký và push
Gắn đúng commit vừa publish:
```bash
git tag -s 1.2.0 -m "1.2.0"
git tag -v 1.2.0                 # Good signature ... 305CA850DB3AD879
git push origin 1.2.0
```

### B5. Xác nhận phát hành xong
Chỉ thông báo phát hành khi file tải được từ Maven Central:
```bash
curl -sf https://repo.maven.apache.org/maven2/io/github/idaplatform/fpt-cmp-sdk/1.2.0/fpt-cmp-sdk-1.2.0.pom \
  >/dev/null && echo "OK: da co tren Maven Central"
```
Thường mất vài phút đến khoảng 30 phút sau trạng thái `PUBLISHED`.

Tạo GitHub Release từ tag `1.2.0`, dán mục `1.2.0` của `CHANGELOG.md`, đính kèm
`fpt-cmp/build/outputs/aar/fpt-cmp-release.aar`.

---

## Phần C — Quy tắc

- **Không ghi đè được.** Version đã lên Central là vĩnh viễn. Lỗi → phát hành `X.Y.(Z+1)`.
- **Không xoá / di chuyển tag** đã push.
- SemVer: đổi hoặc xoá public API → MAJOR; thêm API → MINOR; sửa lỗi → PATCH.
- Version trong tag, `gradle.properties` và README phải giống nhau.
- Không commit token, khoá, file `*.gpg` / `*.asc` (đã có trong `.gitignore`).

---

## Phần D — Lỗi thường gặp

| Lỗi | Nguyên nhân | Cách xử lý |
|---|---|---|
| `gpg: signing failed: Inappropriate ioctl for device` | gpg không mở được ô nhập passphrase | A1; chạy lệnh trong terminal thật |
| Commit trên GitHub hiện **Unverified** | Email commit không có trong khoá, hoặc khoá chưa thêm vào tài khoản GitHub có email đó | A2; `git log -1 --show-signature` phải ra key `…305CA850DB3AD879` |
| Commit ký bằng khoá `…B6212CBC626C1671` | `user.signingkey` chưa đặt khi commit, gpg tự chọn khoá theo email | A2, rồi `git commit --amend --no-edit -S` (chỉ với commit chưa push) |
| Thiếu file `.asc` sau B2 | Gradle không thấy `signingInMemoryKey` | A5 |
| Portal báo *Namespace not verified* / 403 | Namespace `io.github.idaplatform` chưa *Verified*, hoặc token thuộc tài khoản khác | A3 |
| Portal báo *Invalid signature* / *could not find public key* | Khoá artifact chưa có trên keyserver | A4 |
| Portal báo *Component already exists* | Version đã phát hành | Tăng version (Phần C) |
| `401 Unauthorized` khi upload | Sai hoặc hết hạn user token | Tạo token mới (A3 bước 4) |

Tham khảo: [Sonatype — yêu cầu khi publish](https://central.sonatype.org/publish/requirements/),
[Gradle Maven Publish plugin](https://vanniktech.github.io/gradle-maven-publish-plugin/central/).
