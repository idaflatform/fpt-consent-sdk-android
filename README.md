# Tích hợp CMP Consent SDK cho Android — 8 bước

Tài liệu dành cho **lập trình viên app**. SDK phát hành trên **Maven Central** — public, không cần tài
khoản, không cần khai URL repository riêng. Mẫu code có cả **Java** và **Kotlin**, cấu hình Gradle có cả
**Groovy** (`build.gradle`) và **Kotlin DSL** (`build.gradle.kts`) — chọn đúng loại project của bạn.

Cần 2 thông tin:

| Thông tin | Ví dụ |
|---|---|
| Dependency | `io.github.idaflatform:fpt-consent-sdk:<version>` |
| `codeConfig` (integration key của Collection Point) | `cp_xxx::t_yyy` |

> **Phiên bản.** Các bước dưới đây dùng 3 tính năng mới: **tự khởi tạo từ `<meta-data>`**, **tự tải cấu
> hình (`cmpAutoLoad`) + `reload()`**, và **`form.submit(...)`**. Chúng có từ bản kế tiếp sau `1.0.2`
> (dự kiến `1.1.0`). Đang dùng `1.0.x`? Xem [Nâng cấp từ 1.0.x](#nâng-cấp-từ-10x).

## SDK làm gì

```
<meta-data codeConfig>  ──►  SDK tự khởi tạo trước Application.onCreate()
        │
        ▼
ConsentFormView tự tải  ──►  GET  /api/v1/cmp/consent/config?code_config=...
        │                     (danh sách mục đích + trường dữ liệu + bên thứ ba)
        │                     đang tải / lỗi + nút "Thử lại" do SDK tự vẽ
        ▼
Người dùng bật/tắt từng mục
        │
        ▼
App gọi form.submit(...)  ──►  POST /api/v1/cmp/consent/sendData
                               (bản ghi bằng chứng: ai đồng ý gì, lúc nào)
```

SDK lo: khởi tạo, gọi API, dựng UI (kể cả trạng thái tải / lỗi / thử lại), áp quy tắc bắt buộc, chống
gửi trùng, hiện lỗi trên form, lưu quyết định xuống máy, sinh `visitorId`/`consentId`.
App lo: khai `codeConfig`, đặt form đúng chỗ, gọi `submit` ở nút của mình, và bật/tắt tính năng theo
quyết định.

Yêu cầu tối thiểu: `minSdk 21`. Quyền `INTERNET` do SDK tự khai báo.

---

## Bước 1. Bảo đảm project có `mavenCentral()`

SDK nằm trên Maven Central nên **không phải thêm repository nào mới** — mọi project Android đều đã khai
`mavenCentral()` sẵn. Chỉ cần kiểm tra lại:

**Gradle 7+ (`settings.gradle`)**:

```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()      // <- SDK lay tu day
    }
}
```

**Kotlin DSL (`settings.gradle.kts`)**:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
```

Project cũ (repository khai ở `build.gradle` gốc):

```groovy
allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
```

Không cần block `credentials`, không cần proxy/token. Nếu công ty bạn chặn `repo1.maven.org` và dùng
Nexus làm mirror, khai thêm URL mirror đó theo hướng dẫn nội bộ — dependency vẫn giữ nguyên.

## Bước 2. Thêm dependency

`app/build.gradle` (Groovy):

```groovy
dependencies {
    implementation 'io.github.idaflatform:fpt-consent-sdk:<version>'
}
```

`app/build.gradle.kts` (Kotlin DSL):

```kotlin
dependencies {
    implementation("io.github.idaflatform:fpt-consent-sdk:<version>")
}
```

SDK tự kéo theo `androidx.fragment`, `androidx.annotation`, `appcompat`, `core`, `material` — app không
cần khai lại. Nếu app đã dùng phiên bản cao hơn thì Gradle giữ bản cao hơn, không cần `exclude`.

## Bước 3. Kiểm tra cấu hình module app

`app/build.gradle` (Groovy):

```groovy
android {
    defaultConfig {
        minSdk 21          // SDK yeu cau >= 21
    }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8   // hoac 11 / 17
        targetCompatibility JavaVersion.VERSION_1_8
    }
}
```

`app/build.gradle.kts` (Kotlin DSL):

```kotlin
android {
    defaultConfig {
        minSdk = 21        // SDK yeu cau >= 21
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8   // hoac 11 / 17
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
```

Quyền `INTERNET` do SDK tự khai báo — app không cần thêm. Nếu CMP nội bộ còn chạy `http`, chỉ cho phép
cleartext ở **bản debug** — tạo `app/src/debug/AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application android:usesCleartextTraffic="true" />
</manifest>
```

Sync Gradle. Import được `vn.fpt.fis.cmp.consent.ConsentCmp` là xong phần cài đặt.

## Bước 4. Khởi tạo SDK — khai `<meta-data>`, không cần viết code

> **Thay đổi so với bản cũ:** không còn phải tạo class `Application`, khai `android:name` và gọi
> `ConsentCmp.init(...)`. SDK có sẵn một `ContentProvider` — manifest merger tự chèn vào app, chạy
> **trước** `Application.onCreate()` và tự khởi tạo từ `<meta-data>`.

`AndroidManifest.xml` của app:

```xml
<application ...>
    <meta-data android:name="vn.fpt.cmp.codeConfig" android:value="cp_xxx::t_yyy" />
    <!-- tuy chon — bo qua thi dung PROD https://cmp.biznext.vn (khong kem path) -->
    <meta-data android:name="vn.fpt.cmp.baseUrl" android:value="https://uat-cmp.biznext.vn" />
</application>
```

Nên tách theo môi trường bằng `manifestPlaceholders` —

`app/build.gradle` (Groovy):

```groovy
android {
    defaultConfig {
        manifestPlaceholders = [cmpCodeConfig: 'cp_xxx::t_yyy', cmpBaseUrl: 'https://cmp.biznext.vn']
    }
}
```

`app/build.gradle.kts` (Kotlin DSL):

```kotlin
android {
    defaultConfig {
        manifestPlaceholders["cmpCodeConfig"] = "cp_xxx::t_yyy"
        manifestPlaceholders["cmpBaseUrl"] = "https://cmp.biznext.vn"
    }
}
```

rồi trong manifest dùng placeholder:

```xml
<meta-data android:name="vn.fpt.cmp.codeConfig" android:value="${cmpCodeConfig}" />
<meta-data android:name="vn.fpt.cmp.baseUrl" android:value="${cmpBaseUrl}" />
```

| Tình huống | Kết quả |
|---|---|
| Có `codeConfig` | SDK tự khởi tạo |
| `codeConfig` rỗng / không khai | SDK không khởi tạo, **không crash**; `ConsentCmp.isInitialized()` trả `false` |
| `baseUrl` rỗng | dùng PROD `https://cmp.biznext.vn` |

### Khi cần thêm tuỳ chọn (header, timeout, `source`…) — khởi tạo bằng code

Cấu hình bằng code **luôn thắng** `<meta-data>`. Khai `<application android:name=".MyApp" ...>` —

Java:

```java
public class MyApp extends Application {
    @Override public void onCreate() {
        super.onCreate();
        ConsentCmp.init(this, new ConsentOptions.Builder()
                .baseUrl("https://cmp.biznext.vn")   // khong kem path
                .codeConfig("cp_xxx::t_yyy")          // integration key
                .header("X-Api-Key", "...")           // tuy chon
                .build());
    }
}
```

Kotlin:

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ConsentCmp.init(this, ConsentOptions.Builder()
            .baseUrl("https://cmp.biznext.vn")   // khong kem path
            .codeConfig("cp_xxx::t_yyy")          // integration key
            .header("X-Api-Key", "...")           // tuy chon
            .build())
    }
}
```

Muốn tắt hẳn tự khởi tạo:

```xml
<provider android:name="vn.fpt.fis.cmp.consent.ConsentInitProvider"
    android:authorities="${applicationId}.cmp-init" tools:node="remove" />
```

## Bước 5. Hiện màn hình đồng ý

Có 2 cách: **nhúng form vào layout** (thường dùng ở màn đăng ký) hoặc **bottom sheet** (màn khởi động /
nút "Cài đặt quyền riêng tư").

### 5a. Nhúng form vào layout — form tự tải cấu hình

> **Thay đổi so với bản cũ:** không còn phải tự gọi `fetchConfig(...)` → `form.bind(...)` → tự xử lý lỗi.
> Bật `app:cmpAutoLoad="true"` là form tự gọi `/config` khi hiện lên màn hình, tự hiện "Đang tải…", và
> khi lỗi thì tự hiện thông báo + nút **Thử lại**.

```xml
<!-- Nho khai xmlns:app="http://schemas.android.com/apk/res-auto" o the goc cua layout -->
<vn.fpt.fis.cmp.consent.ui.ConsentFormView
    android:id="@+id/consentForm"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:cmpAutoLoad="true"
    app:cmpEmbeddedInForm="true" />
```

Tạo form bằng code (Compose, framework đa nền tảng…) thì dùng `setAutoLoad(true)` /
`setEmbeddedInForm(true)` thay cho thuộc tính XML.

Tuỳ chọn — chỉ bật nút của app khi form đã tải xong; tải lại bằng code với `form.reload()`:

Java:

```java
ConsentFormView form = findViewById(R.id.consentForm);
btnRegister.setEnabled(false);
form.setOnLoadListener(new ConsentFormView.OnLoadListener() {
    @Override public void onLoaded(ConsentConfig config) { btnRegister.setEnabled(true); }
    @Override public void onLoadFailed(ConsentException e) { /* SDK da hien nut Thu lai */ }
});
// form.reload();   // tai lai cau hinh bat ky luc nao
```

Kotlin:

```kotlin
val form = findViewById<ConsentFormView>(R.id.consentForm)
btnRegister.isEnabled = false
form.setOnLoadListener(object : ConsentFormView.OnLoadListener {
    override fun onLoaded(config: ConsentConfig) { btnRegister.isEnabled = true }
    override fun onLoadFailed(error: ConsentException) { /* SDK da hien nut Thu lai */ }
})
// form.reload()    // tai lai cau hinh bat ky luc nao
```

Mất mạng: SDK dùng tạm bản cấu hình đã tải lần trước (nếu có). Form `inactive` trên portal: `onLoaded`
vẫn được gọi, app kiểm tra `config.isActive()` để quyết định có hiện bước consent không.

**Nút gửi của SDK hiện hay ẩn tuỳ màn hình của bạn có phải là form hay không:**

| | Màn hình **không** phải form | Màn hình **là** form của app |
|---|---|---|
| Khai báo | mặc định, không cần khai gì | `app:cmpEmbeddedInForm="true"` |
| Nút "Từ chối tất cả" / "Đồng ý" của SDK | **hiện** | **ẩn** |
| Ai gửi `/sendData` | **SDK tự gửi** khi người dùng bấm nút | **app gọi** `form.submit(...)` — Bước 6 |
| Nhận kết quả | `form.setOnSubmitResultListener(...)` | callback của `form.submit(...)` |

### 5b. Bottom sheet

Cần `FragmentActivity` (hoặc lớp con như `AppCompatActivity`). Sheet tự tải cấu hình, tự gửi khi người
dùng bấm nút.

Java:

```java
ConsentCmp.get().showIfNeeded(getSupportFragmentManager(), new ConsentListener() {
    @Override public void onCompleted(ConsentState state, @Nullable SendConsentResult result) {
        // result != null: vua gui len server; result == null: doc lai quyet dinh da luu
        applyConsent(state);
    }
    @Override public void onDismissed() { /* dong ma chua luu -> lan mo app sau hoi lai */ }
    @Override public void onError(ConsentException error) {
        Log.w("CMP", error.getMessage() + " http=" + error.httpStatus);
    }
});
```

Kotlin:

```kotlin
ConsentCmp.get().showIfNeeded(supportFragmentManager, object : ConsentListener {
    override fun onCompleted(state: ConsentState, result: SendConsentResult?) {
        // result != null: vua gui len server; result == null: doc lai quyet dinh da luu
        applyConsent(state)
    }
    override fun onDismissed() { /* dong ma chua luu -> lan mo app sau hoi lai */ }
    override fun onError(error: ConsentException) {
        Log.w("CMP", "${error.message} http=${error.httpStatus}")
    }
})
```

| Hàm | Khi nào dùng |
|---|---|
| `showIfNeeded(fm, listener)` | màn hình khởi động — chỉ hỏi khi người dùng chưa từng trả lời |
| `show(fm, listener)` | nút "Cài đặt quyền riêng tư" — luôn mở để sửa lựa chọn |

## Bước 6. Gửi consent và giá trị người dùng đã nhập — `form.submit(...)`

> **Thay đổi so với bản cũ:** không còn phải tự `validateRequired()` → `setSubmitting(true)` →
> `ConsentCmp.submit(...)` → `setSubmitting(false)` + `setError(...)`. Nút của app chỉ cần gọi **một hàm**
> `form.submit(...)`; SDK tự kiểm tra mục bắt buộc, gọi `/sendData` và hiện lỗi ngay trên form.

Java:

```java
btnRegister.setOnClickListener(v -> form.submit(
        new MapValueSource()
                .put("Email", edtEmail.getText().toString())
                .put("PhoneNumber", edtPhone.getText().toString()),
        new ConsentCallback<SendConsentResult>() {
            @Override public void onSuccess(SendConsentResult r) {
                registerAccount(r.consentId);    // gui consent truoc, tao tai khoan sau
            }
            @Override public void onError(ConsentException e) {
                // SDK da hien loi ngay tren form — app chi can dung luong cua minh
            }
        }));
```

Kotlin:

```kotlin
btnRegister.setOnClickListener {
    form.submit(
        MapValueSource()
            .put("Email", edtEmail.text.toString())
            .put("PhoneNumber", edtPhone.text.toString()),
        object : ConsentCallback<SendConsentResult> {
            override fun onSuccess(result: SendConsentResult) {
                registerAccount(result.consentId)   // gui consent truoc, tao tai khoan sau
            }
            override fun onError(error: ConsentException) {
                // SDK da hien loi ngay tren form — app chi can dung luong cua minh
            }
        })
}
```

`form.submit(...)` tự làm:

| Tình huống | SDK làm gì |
|---|---|
| Thiếu mục/trường bắt buộc | hiện lỗi trên form, cuộn tới dòng lỗi, gọi `onError`, **không** gọi mạng |
| Form chưa tải xong cấu hình | gọi `onError` "Chưa tải được cấu hình consent" |
| Đang gửi | khoá mọi toggle + hiện vòng quay; **bấm lặp bị bỏ qua** (không tạo 2 bản ghi) |
| Gửi lỗi | hiện lỗi trên form, mở khoá lại, gọi `onError` (`httpStatus == 0` là lỗi mạng) |
| Gửi thành công | gọi `onSuccess` với `consentId` mới, lưu quyết định xuống máy |

Callback luôn chạy trên main thread. Lần bấm lặp bị bỏ qua **không** gọi callback — nếu app đang chờ
kết quả (spinner riêng, cầu nối sang Flutter / React Native…) thì kiểm tra `form.isSubmitting()` trước.

> **Dễ sót nhất:** đặt `app:cmpEmbeddedInForm="true"` mà quên gọi `form.submit(...)` → người dùng bật/tắt
> xong nhưng **không có bản ghi nào** trên portal.

### Giá trị trường dữ liệu

Trường có `sharedWithSystem = true` cần giá trị thật để làm bằng chứng. SDK đối chiếu theo **`name` của
trường trên portal** (không phân biệt hoa thường; thêm `dataType` nếu cần phân biệt hai trường trùng tên)
— app không cần biết id. Thứ tự SDK tìm giá trị:

1. `values` truyền vào `form.submit(values, callback)` — như ví dụ trên
2. nguồn đã khai bằng `ConsentCmp.get().bindForm(view)` / `setValueSource(...)` — gọi `form.submit(callback)`
3. không khai gì → SDK tự quét các ô nhập có `android:tag` trên cùng màn hình

```xml
<EditText android:id="@+id/edtName"  android:tag="full_name" />
<EditText android:id="@+id/edtEmail" android:tag="Email|EMAIL" />   <!-- name|dataType -->
```

Giá trị được gửi kèm **bất kể toggle bật hay tắt**; `isAccept` mới là lựa chọn của người dùng.

> **Trường ẩn (`display = false`).** `/config` trả về **mọi** trường của form dữ liệu nguồn, nhưng chỉ
> trường được chọn cho mục đích đó trong template mới có `display = true`. Trường `display = false`: SDK
> **không** vẽ toggle, **luôn** gửi `isAccept = false`, **không** tính vào ràng buộc bắt buộc — nhưng
> **vẫn** gửi `value` nếu `sharedWithSystem = true` và app có truyền giá trị. Muốn trường được hỏi người
> dùng → chọn trường đó cho mục đích trên portal; app không cần sửa gì.

Không biết `name` của trường? In cấu hình ở Bước 7.

## Bước 7. Gate tính năng theo quyết định

Java:

```java
boolean granted   = ConsentCmp.get().isGranted(PURPOSE_KEY);
ConsentState last = ConsentCmp.get().savedState();
String consentId  = last != null ? last.getConsentId() : null;   // dinh kem khi goi API nghiep vu
ConsentCmp.get().clear();      // khi dang xuat — visitorId van giu nguyen
```

Kotlin:

```kotlin
val granted   = ConsentCmp.get().isGranted(PURPOSE_KEY)
val consentId = ConsentCmp.get().savedState()?.consentId          // dinh kem khi goi API nghiep vu
ConsentCmp.get().clear()       // khi dang xuat — visitorId van giu nguyen
```

Lấy `PURPOSE_KEY` và `name` của trường bằng cách in cấu hình một lần (bản debug):

Java:

```java
ConsentCmp.get().fetchConfig(new ConsentCallback<ConsentConfig>() {
    @Override public void onSuccess(ConsentConfig config) {
        for (ConsentItem item : config.items()) {
            Log.d("CMP", "purpose " + item.key() + " = " + item.label);
            for (ConsentField f : item.dataFields) {
                Log.d("CMP", "  field name=" + f.name + " type=" + f.dataType
                        + " display=" + f.display + " shared=" + f.sharedWithSystem);
            }
        }
    }
    @Override public void onError(ConsentException e) { }
});
```

Kotlin:

```kotlin
ConsentCmp.get().fetchConfig(object : ConsentCallback<ConsentConfig> {
    override fun onSuccess(config: ConsentConfig) {
        config.items().forEach { item ->
            Log.d("CMP", "purpose ${item.key()} = ${item.label}")
            item.dataFields.forEach { f ->
                Log.d("CMP", "  field name=${f.name} type=${f.dataType} display=${f.display} shared=${f.sharedWithSystem}")
            }
        }
    }
    override fun onError(error: ConsentException) {}
})
```

## Bước 8. Kiểm tra trước khi phát hành app

- [ ] `<meta-data>` `codeConfig` / `baseUrl` đúng môi trường (xem tab **Merged Manifest** trong Android Studio)
- [ ] Mở màn hình → form tự tải đúng danh sách mục đích như trên portal CMP
- [ ] Tắt mạng rồi mở → có lỗi + nút **Thử lại**; bật mạng, bấm Thử lại → tải được
- [ ] Nút của app có gọi `form.submit(...)`; submit xong thấy bản ghi mới trong báo cáo consent,
      `consentId` khớp giá trị app nhận được
- [ ] Trường `sharedWithSystem = true` có `value` trong bản ghi (tên truyền vào khớp `name` trên portal)
- [ ] Tắt toggle mục đích → bản ghi có `isAccept = false` cho mục đích và mọi trường con
- [ ] Bấm nút liên tục → chỉ **1** bản ghi
- [ ] Đã gate analytics/ads bằng `isGranted(...)`; `clear()` được gọi khi đăng xuất
- [ ] Bản release **không** bật `usesCleartextTraffic`

---

## Lỗi thường gặp

| Lỗi | Nguyên nhân | Xử lý |
|---|---|---|
| `Could not find io.github.idaflatform:fpt-consent-sdk:<version>` | thiếu `mavenCentral()`, sai groupId, hoặc version không tồn tại | xem lại bước 1–2; hỏi đội CMP version hiện có |
| `Build was configured to prefer settings repositories` | project dùng `dependencyResolutionManagement` nhưng repo lại khai ở `build.gradle` | chuyển khai báo repo vào `settings.gradle` |
| `Could not resolve ... repo1.maven.org` | mạng công ty chặn Maven Central | dùng URL mirror Nexus nội bộ, dependency giữ nguyên |
| `minSdkVersion 16 cannot be smaller than version 21` | app có `minSdk` thấp hơn SDK | nâng `minSdk` lên 21 |
| Không tìm thấy `submit` / `setAutoLoad` / `reload` khi compile | đang dùng bản `1.0.x` | nâng version — xem [Nâng cấp từ 1.0.x](#nâng-cấp-từ-10x) |
| Form không hiện, `ConsentCmp.isInitialized()` trả `false` | thiếu / rỗng `<meta-data vn.fpt.cmp.codeConfig>` | xem lại bước 4, kiểm tra Merged Manifest |
| `IllegalStateException: ConsentCmp chua duoc khoi tao` | gọi `ConsentCmp.get()` khi SDK chưa khởi tạo | như trên, hoặc kiểm tra `isInitialized()` trước |
| `Chưa tải được cấu hình consent` khi submit | bấm nút trước khi form tải xong `/config` | chỉ bật nút sau `onLoaded` (bước 5a) |
| Crash khi mở màn hình consent, log nhắc tới Material attrs | theme app thiếu attrs của Material | SDK đã tự bọc Material Bridge; nếu vẫn lỗi, đổi theme app sang `Theme.MaterialComponents.*.Bridge` |
| `CLEARTEXT communication not permitted` lúc chạy app | CMP nội bộ dùng `http` | bật cleartext cho bản debug (bước 3) |
| Bấm nút nhưng không có bản ghi consent nào | `app:cmpEmbeddedInForm="true"` mà app chưa gọi `form.submit(...)` | gọi `form.submit(...)` ở nút của app (bước 6) |
| Bản ghi thiếu `value` của một trường | tên truyền vào không khớp `name` trên portal, hoặc trường không `sharedWithSystem` | in cấu hình (bước 7) để lấy đúng `name` |
| Trường có trên portal nhưng form không hiện toggle | trường đó `display = false` với mục đích này | chọn trường cho mục đích trên portal |
| Đổi cấu hình trên portal mà app chưa thấy | app tự gọi `fetchConfig(true, ...)` (ưu tiên cache) | autoload / `fetchConfig(callback)` luôn gọi mạng; cần chắc thì gọi `invalidateConfigCache()` trước |

## Mục bắt buộc — hành vi app cần biết

`/config` có thể đánh dấu `required = true` ở **mục đích** hoặc ở **trường dữ liệu**. SDK xử lý sẵn,
nhưng app nên hiểu để không thấy lạ:

| Tình huống | SDK làm gì |
|---|---|
| Mục đích `required = true` | bật sẵn khi mở; người dùng tắt thì tự bật lại kèm thông báo |
| Mục đích chứa trường `required = true` (đang hiển thị) | cũng **không tắt được** — về "trạng thái tối thiểu": mục đích bật, chỉ trường bắt buộc bật |
| Trường `required = true`, mục đích cha đang bật | không tắt riêng được; tắt thì tự bật lại kèm thông báo |
| "Từ chối tất cả" khi còn mục bắt buộc | tắt hết phần còn lại, giữ phần bắt buộc ở mức tối thiểu, hiện lý do |
| `form.submit(...)` khi còn mục bắt buộc chưa bật | lỗi hiện trên form, **không gọi mạng**; backend chặn lần hai |

Mặc định khi mở màn hình: chỉ mục/trường `required` (và mục đích `defaultChecked`) bật sẵn, còn lại tắt.
Người dùng đã trả lời trước đó thì form hiện lại lựa chọn cũ.

## Tham chiếu API nhanh

| Gọi gì | Khi nào |
|---|---|
| `<meta-data>` `vn.fpt.cmp.codeConfig` / `vn.fpt.cmp.baseUrl` | tự khởi tạo — không cần code |
| `ConsentCmp.init(context, options)` | khởi tạo bằng code khi cần header/timeout — thắng `<meta-data>` |
| `ConsentCmp.isInitialized()` | kiểm tra SDK đã khởi tạo chưa |
| `showIfNeeded(fm, listener)` | màn hình khởi động — chỉ hỏi khi người dùng chưa trả lời |
| `show(fm, listener)` | nút "Cài đặt quyền riêng tư" — luôn mở |
| `fetchConfig(callback)` / `fetchConfig(true, callback)` | tự tải cấu hình (luôn gọi mạng / ưu tiên cache) |
| `invalidateConfigCache()` | ép bỏ cache trước khi tải lại |
| `bindForm(view)` / `setValueSource(...)` | khai nguồn giá trị người dùng nhập mặc định |
| `submit(state, callback)` | gửi từ state tự dựng (không qua `ConsentFormView`) |
| `isGranted(purposeKey)` | gate analytics/ads ở bất kỳ đâu |
| `hasConsented()` / `savedState()` | đọc lại quyết định đã lưu |
| `clear()` | đăng xuất — xoá quyết định, giữ `visitorId` |

`ConsentFormView`:

| XML | Code | Khi nào |
|---|---|---|
| `app:cmpAutoLoad="true"` | `setAutoLoad(true)` | tự tải `/config`, lỗi thì hiện nút Thử lại |
| — | `reload()` | tải lại cấu hình |
| — | `setOnLoadListener(...)` | biết khi nào form tải xong / lỗi |
| `app:cmpEmbeddedInForm="true"` | `setEmbeddedInForm(true)` | màn hình là form của app → ẩn nút SDK, app gọi `submit` |
| — | `submit(callback)` / `submit(values, callback)` | nút của app — kiểm tra bắt buộc + gửi + hiện lỗi |
| — | `isSubmitting()` | đang gửi |
| — | `setOnStateChangeListener(...)` | bật/tắt nút "Đăng ký" theo `allRequiredGranted` |
| — | `validateRequired()` / `requiredMessage()` | tự kiểm tra bắt buộc (không cần nếu dùng `submit`) |
| — | `setOnSubmitResultListener(...)` | nhận kết quả khi SDK tự gửi (màn hình không phải form) |
| — | `bind(config, state)` | tự dựng form khi không dùng autoload |
| `app:cmpThirdPartiesOnSharedOnly="true"` | `setThirdPartiesOnSharedOnly(true)` | chỉ hiện chip bên thứ ba ở trường được chia sẻ |
| `app:cmpShowSectionTitle="false"` | `setSectionTitleVisible(false)` | ẩn dòng tiêu đề nhỏ phía trên |

## Nâng cấp version SDK

1. Đội CMP thông báo version mới + thay đổi (mục nào phá vỡ tương thích).
2. App đổi số version trong `build.gradle` / `build.gradle.kts`, sync, build.
3. Đọc kỹ khi **major** tăng: chữ ký hàm hoặc cấu trúc `values` có thể đã đổi.
4. Test lại luồng: hiện màn hình → submit → đối chiếu bản ghi trên portal.

### Nâng cấp từ 1.0.x

Code cũ **vẫn chạy** — các hàm cũ được giữ nguyên. Muốn rút gọn theo cách mới:

| Bản 1.0.x | Thay bằng |
|---|---|
| Class `Application` gọi `ConsentCmp.init(...)` + `android:name` + `buildConfigField` | 2 dòng `<meta-data>` (bước 4) — xoá class nếu chỉ dùng để init |
| `fetchConfig(...)` → `form.bind(config, savedState())` → tự xử lý lỗi | `app:cmpAutoLoad="true"` (bước 5a) |
| `validateRequired()` → `setSubmitting(true)` → `ConsentCmp.submit(form.state(), ...)` → `setSubmitting(false)` + `setError(...)` | `form.submit(values, callback)` (bước 6) |
| `setValueSource(...)` trước khi submit | truyền thẳng `values` vào `form.submit(...)` |

Hành vi khác cần biết: `ConsentCmp.submit(state, callback)` giờ **báo lỗi** nếu SDK chưa tải được cấu
hình lần nào (trước đây gửi luôn mà không kiểm tra mục bắt buộc).

## Liên hệ / hỗ trợ

Gửi kèm khi báo lỗi cho đội CMP: `codeConfig`, `consentId` nhận được, thời điểm, và log tag `CMP`.

---
