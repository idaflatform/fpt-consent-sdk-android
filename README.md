# Tích hợp CMP Consent SDK cho Android — 8 bước

Tài liệu dành cho **lập trình viên app**. SDK phát hành trên **Maven Central** — public, không cần tài
khoản, không cần khai URL repository riêng.

Cần 2 thông tin:

| Thông tin | Ví dụ |
|---|---|
| Dependency | `io.github.idaflatform:fpt-consent-sdk:1.0.0` |
| `codeConfig` (integration key của Collection Point) | `cp_xxx::t_yyy` |

## SDK làm gì

```
App gọi fetchConfig()  ──►  GET  /api/v1/cmp/consent/config?code_config=...
                                 (danh sách mục đích + trường dữ liệu + bên thứ ba)
        │
        ▼
SDK dựng màn hình "Danh sách sự đồng ý"  ──►  người dùng bật/tắt từng mục
        │
        ▼
App (hoặc SDK) gọi submit()  ──►  POST /api/v1/cmp/consent/sendData
                                  (bản ghi bằng chứng: ai đồng ý gì, lúc nào)
```

SDK lo: gọi API, dựng UI, áp quy tắc bắt buộc, lưu quyết định xuống máy, sinh `visitorId`/`consentId`.
App lo: khởi tạo 1 lần, mở màn hình đúng chỗ, và bật/tắt tính năng theo quyết định.

Yêu cầu tối thiểu: `minSdk 21`, màn hình gọi SDK là `AppCompatActivity`/`FragmentActivity`.
Quyền `INTERNET` do SDK tự khai báo.

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

**Project cũ (repository khai ở `build.gradle` gốc)**:

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

`app/build.gradle`:

```groovy
dependencies {
    implementation 'io.github.idaflatform:fpt-consent-sdk:1.0.0'
}
```

Kotlin DSL:

```kotlin
implementation("io.github.idaflatform:fpt-consent-sdk:1.0.0")
```

SDK tự kéo theo `androidx.fragment`, `androidx.annotation`, `appcompat`, `core`, `material` — app không
cần khai lại. Nếu app đã dùng phiên bản cao hơn thì Gradle giữ bản cao hơn, không cần `exclude`.

## Bước 3. Kiểm tra cấu hình module app

```groovy
android {
    defaultConfig {
        minSdk 21          // SDK yeu cau >= 21
    }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8   // hoac 17
        targetCompatibility JavaVersion.VERSION_1_8
    }
}
```

Quyền `INTERNET` do SDK tự khai báo — app không cần thêm. Nếu CMP nội bộ còn chạy `http`, app phải cho
phép cleartext cho host đó (`network_security_config.xml`, hoặc `android:usesCleartextTraffic="true"`
chỉ ở bản debug).

Sync Gradle. Import được `vn.fpt.fis.cmp.consent.ConsentCmp` là xong phần cài đặt.

## Bước 4. Khởi tạo SDK

```java
public class MyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ConsentCmp.init(this, new ConsentOptions.Builder()
                .baseUrl("https://cmp.biznext.vn")   // khong kem path
                .codeConfig("cp_xxx::t_yyy")             // integration key
                .build());
    }
}
```

Khai báo trong `AndroidManifest.xml`:

```xml
<application android:name=".MyApp" ... />
```

Nên tách theo môi trường bằng `buildConfigField`:

```groovy
android {
    buildFeatures { buildConfig true }
    defaultConfig {
        buildConfigField 'String', 'CMP_BASE_URL', '"https://cmp.biznext.vn"'
        buildConfigField 'String', 'CMP_CODE_CONFIG', '"cp_xxx::t_yyy"'
    }
}
```

## Bước 5. Hiện màn hình đồng ý

Trong Activity đầu tiên (`AppCompatActivity` / `FragmentActivity`):

```java
ConsentCmp.get().showIfNeeded(getSupportFragmentManager(), new ConsentListener() {

    @Override
    public void onCompleted(ConsentState state, @Nullable SendConsentResult result) {
        // result != null: vua gui len server; result == null: doc lai quyet dinh da luu
        applyConsent(state);
    }

    @Override
    public void onDismissed() { /* dong ma chua luu -> lan mo app sau hoi lai */ }

    @Override
    public void onError(ConsentException error) {
        Log.w("CMP", error.getMessage() + " http=" + error.httpStatus);
    }
});
```

| Hàm | Khi nào dùng |
|---|---|
| `showIfNeeded(fm, listener)` | màn hình khởi động — chỉ hỏi khi người dùng chưa từng trả lời |
| `show(fm, listener)` | nút "Cài đặt quyền riêng tư" — luôn mở để sửa lựa chọn |

### Nhúng thẳng vào layout thay vì bottom sheet — và ai gửi dữ liệu

Dùng `ConsentFormView` khi muốn khối "Danh sách sự đồng ý" nằm ngay trong màn hình. **Nút gửi của SDK
hiện hay ẩn tuỳ layout của bạn có phải là form hay không** — đây cũng là điểm quyết định thời điểm dữ
liệu được gửi lên `/sendData`:

| | Layout **không** phải form | Layout **là** form của app |
|---|---|---|
| Khai báo | mặc định, không cần khai gì | `app:cmpEmbeddedInForm="true"` |
| Nút "Từ chối tất cả" / "Đồng ý" của SDK | **hiện** (`visibility = visible`) | **ẩn** (`visibility = false/gone`) |
| Ai gửi `/sendData` | **SDK tự gửi** khi người dùng bấm nút | **app gọi** `ConsentCmp.submit(...)` |
| Thời điểm gửi | ngay lúc người dùng bấm nút của SDK | cùng lúc app gửi form của mình (1 nút duy nhất) |
| Nhận kết quả | `form.setOnSubmitResultListener(...)` | callback của `ConsentCmp.submit(...)` |

**Layout là form → nút SDK bị ẩn, app phải tự gửi.** Đây là điểm dễ bỏ sót nhất: khi đặt
`app:cmpEmbeddedInForm="true"`, SDK **không** gọi `/sendData` nữa — nếu app quên gọi `submit(...)` thì
người dùng bật/tắt toggle xong sẽ không có bản ghi nào được tạo, màn hình vẫn trông như đã lưu.

```xml
<!-- Nho khai xmlns:app="http://schemas.android.com/apk/res-auto" o the goc cua layout -->
<vn.fpt.fis.cmp.consent.ui.ConsentFormView
    android:id="@+id/consentForm"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:cmpEmbeddedInForm="true" />
```

```java
final ConsentFormView form = findViewById(R.id.consentForm);

ConsentCmp.get().fetchConfig(new ConsentCallback<ConsentConfig>() {
    @Override public void onSuccess(ConsentConfig config) {
        form.bind(config, ConsentCmp.get().savedState());
    }
    @Override public void onError(ConsentException e) { form.setError(e.getMessage()); }
});

// Nut cua APP -> app tu goi submit; gui consent truoc, tao tai khoan sau.
btnRegister.setOnClickListener(v -> {
    if (!form.validateRequired()) return;
    form.setSubmitting(true);
    ConsentCmp.get().submit(form.state(), new ConsentCallback<SendConsentResult>() {
        @Override public void onSuccess(SendConsentResult r) {
            form.setSubmitting(false);
            registerAccount(r.consentId);
        }
        @Override public void onError(ConsentException e) {
            form.setSubmitting(false);
            form.setError(e.getMessage());
        }
    });
});
```

Layout không phải form thì bỏ `app:cmpEmbeddedInForm` — SDK hiện nút và tự gửi, app chỉ cần
`form.setOnSubmitResultListener(...)` nếu muốn biết kết quả. Muốn giữ nút của SDK nhưng tự xử lý việc
gửi thì đặt `form.setOnActionListener(...)`; khi đó SDK không tự gọi `/sendData`.

Code đầy đủ cho cả 2 chế độ: [INTEGRATION.md mục 7](INTEGRATION.md).

## Bước 6. Gửi kèm giá trị người dùng đã nhập

Trường có `sharedWithSystem = true` cần giá trị thật để làm bằng chứng. Gắn `android:tag` cho input rồi
trỏ SDK vào form:

```xml
<LinearLayout android:id="@+id/formRoot" ...>
    <EditText android:id="@+id/edtName"  android:tag="full_name" />
    <EditText android:id="@+id/edtEmail" android:tag="email|EMAIL" />   <!-- name|dataType -->
</LinearLayout>
```

```java
ConsentCmp.get().bindForm(findViewById(R.id.formRoot));
```

Hoặc truyền tay:

```java
ConsentCmp.get().setValueSource(new MapValueSource()
        .put("full_name", edtName.getText().toString())
        .put("email", "EMAIL", edtEmail.getText().toString()));
```

SDK đối chiếu theo `name` (+ `dataType`), app không cần biết id của trường. Giá trị được gửi kèm **bất kể
toggle bật hay tắt**; `isAccept` mới là lựa chọn của người dùng.

> **Trường ẩn (`display = false`).** `/config` trả về **mọi** trường của form dữ liệu nguồn, nhưng chỉ
> trường được chọn cho mục đích đó trong template mới có `display = true`. Trường `display = false` vẫn
> nằm trong `item.dataFields` để app biết form nguồn thu những gì, nhưng SDK **không** dựng UI cho nó,
> **không** bao giờ đặt `isAccept = true` và **không** tính vào ràng buộc bắt buộc. Nhưng SDK **vẫn**
> quét form để gửi `value` khi `sharedWithSystem = true` — bản ghi phải thể hiện đúng dữ liệu nào đã
> được chia sẻ, nên trường ẩn vẫn có khoá trong `values` với `isAccept = false`. Nếu app tự duyệt
> `item.dataFields`, hãy tự bỏ qua trường có `display = false` khi dựng UI. Backend cũ không trả khoá
> này — khi đó SDK coi như `display = true`.

## Bước 7. Gate tính năng theo quyết định

```java
private void applyConsent(ConsentState state) {
    if (state.isGranted(PURPOSE_MARKETING)) {
        FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(true);
    }
}
```

Đọc ở bất kỳ đâu, không cần listener:

```java
boolean granted   = ConsentCmp.get().isGranted(PURPOSE_MARKETING);
ConsentState last = ConsentCmp.get().savedState();
String consentId  = last != null ? last.getConsentId() : null;   // dinh kem khi goi API nghiep vu
ConsentCmp.get().clear();      // khi dang xuat — visitorId van giu nguyen
```

Lấy hằng số id bằng cách in cấu hình một lần:

```java
ConsentCmp.get().fetchConfig(new ConsentCallback<ConsentConfig>() {
    @Override public void onSuccess(ConsentConfig config) {
        for (ConsentItem item : config.items()) {
            Log.d("CMP", "purpose " + item.key() + " = " + item.label);
            for (ConsentField f : item.dataFields) {
                Log.d("CMP", "  field " + f.key() + " name=" + f.name
                        + " type=" + f.dataType + " shared=" + f.sharedWithSystem);
            }
        }
    }
    @Override public void onError(ConsentException e) { }
});
```

## Bước 8. Kiểm tra trước khi phát hành app

- [ ] `baseUrl` / `codeConfig` có đúng không?
- [ ] Màn hình consent hiện đúng danh sách mục đích như trên portal CMP
- [ ] Submit xong thấy bản ghi mới trong báo cáo consent, `consentId` khớp giá trị app nhận được
- [ ] Nếu dùng `app:cmpEmbeddedInForm="true"` (nút SDK bị ẩn): đã gọi `ConsentCmp.submit(...)` ở nút của
      app — bấm nút mà không có bản ghi nào trên portal là dấu hiệu quên bước này
- [ ] Trường `sharedWithSystem = true` có `value` trong bản ghi
- [ ] Tắt toggle mục đích → bản ghi có `isAccept = false` cho mục đích và mọi trường con
- [ ] Đã gate analytics/ads bằng `isGranted(...)`
- [ ] `clear()` được gọi khi đăng xuất
- [ ] Bản release **không** bật `usesCleartextTraffic`

---

## Lỗi thường gặp

| Lỗi | Nguyên nhân | Xử lý |
|---|---|---|
| `Could not find io.github.idaflatform:fpt-consent-sdk:1.0.0` | thiếu `mavenCentral()`, sai groupId, hoặc version không tồn tại | xem lại bước 1–2; hỏi đội CMP version hiện có |
| `Build was configured to prefer settings repositories` | project dùng `dependencyResolutionManagement` nhưng repo lại khai ở `build.gradle` | chuyển khai báo repo vào `settings.gradle` |
| `Could not resolve ... repo1.maven.org` | mạng công ty chặn Maven Central | dùng URL mirror Nexus nội bộ, dependency giữ nguyên |
| `minSdkVersion 16 cannot be smaller than version 21` | app có `minSdk` thấp hơn SDK | nâng `minSdk` lên 21 |
| `IllegalStateException: ConsentCmp.init(...) chưa được gọi` | quên `android:name=".MyApp"` trong manifest | xem lại bước 4 |
| Crash khi mở màn hình consent, log nhắc tới Material attrs | theme app thiếu attrs của Material | SDK đã tự bọc Material Bridge; nếu vẫn lỗi, đổi theme app sang `Theme.MaterialComponents.*.Bridge` |
| `CLEARTEXT communication not permitted` lúc chạy app | CMP nội bộ dùng `http` | thêm `network_security_config` cho host đó |
| Bấm nút nhưng không có bản ghi consent nào | layout đặt `app:cmpEmbeddedInForm="true"` (nút SDK bị ẩn) mà app chưa gọi `ConsentCmp.submit(...)` | gọi `submit(...)` ở nút của app, xem Bước 5 |
| Đổi cấu hình trên portal mà app chưa thấy | bản build cũ dùng cache, hoặc app tự gọi `fetchConfig(true, ...)` (ưu tiên cache) | `fetchConfig(callback)` luôn gọi mạng; cần chắc chắn thì gọi `invalidateConfigCache()` trước |

## Mục bắt buộc — hành vi app cần biết

`/config` có thể đánh dấu `required = true` ở **mục đích** hoặc ở **trường dữ liệu**. SDK xử lý sẵn,
nhưng app nên hiểu để không thấy lạ:

| Tình huống | SDK làm gì |
|---|---|
| Mục đích `required = true` | bật sẵn khi mở; người dùng tắt thì tự bật lại kèm thông báo |
| Mục đích chứa trường `required = true` | cũng **không tắt được** — về "trạng thái tối thiểu": mục đích bật, chỉ trường bắt buộc bật |
| Trường `required = true`, mục đích cha đang bật | không tắt riêng được; tắt thì tự bật lại kèm thông báo |
| "Từ chối tất cả" khi còn mục bắt buộc | tắt hết phần còn lại, giữ phần bắt buộc ở mức tối thiểu, hiện lý do |
| Submit khi còn mục bắt buộc chưa bật | `submit()` trả lỗi **trước khi gọi mạng**; backend chặn lần hai |

Mặc định khi mở màn hình: **chỉ** mục/trường `required` bật sẵn, còn lại tắt.

## Tham chiếu API nhanh

| Gọi gì | Khi nào |
|---|---|
| `ConsentCmp.init(context, options)` | một lần trong `Application.onCreate()` |
| `showIfNeeded(fm, listener)` | màn hình khởi động — chỉ hỏi khi người dùng chưa trả lời |
| `show(fm, listener)` | nút "Cài đặt quyền riêng tư" — luôn mở |
| `fetchConfig(callback)` | khi tự dựng UI bằng `ConsentFormView`; **luôn gọi mạng** |
| `fetchConfig(true, callback)` | ưu tiên cache (nhanh, có thể cũ) |
| `invalidateConfigCache()` | ép bỏ cache trước khi tải lại |
| `bindForm(view)` / `setValueSource(...)` | khai nơi lấy giá trị người dùng nhập |
| `submit(state, callback)` | gửi consent (chế độ nhúng trong form của app) |
| `isGranted(purposeKey)` | gate analytics/ads ở bất kỳ đâu |
| `hasConsented()` / `savedState()` | đọc lại quyết định đã lưu |
| `clear()` | đăng xuất — xoá quyết định, giữ `visitorId` |

`ConsentFormView` (khi nhúng vào layout):

| Gọi gì | Khi nào |
|---|---|
| `app:cmpEmbeddedInForm="true"` | layout là form của app → ẩn nút SDK, app tự `submit` |
| `setOnStateChangeListener(...)` | bật/tắt nút "Đăng ký" theo `allRequiredGranted` |
| `validateRequired()` / `requiredMessage()` | kiểm tra + lấy thông báo trước khi submit |
| `setOnSubmitResultListener(...)` | nhận kết quả khi SDK tự gửi (chế độ độc lập) |
| `app:cmpThirdPartiesOnSharedOnly="true"` | chỉ hiện chip bên thứ ba ở trường được chia sẻ |

## Nâng cấp version SDK

1. Đội CMP thông báo version mới + thay đổi (mục nào phá vỡ tương thích).
2. App đổi số version trong `build.gradle`, sync, build.
3. Đọc kỹ khi **major** tăng: chữ ký hàm hoặc cấu trúc `values` có thể đã đổi.
4. Test lại luồng: hiện màn hình → submit → đối chiếu bản ghi trên portal.

## Liên hệ / hỗ trợ

Gửi kèm khi báo lỗi cho đội CMP: `codeConfig`, `consentId` nhận được, thời điểm, và log tag `CMP`.

---
