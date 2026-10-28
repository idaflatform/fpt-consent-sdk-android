# Kotlin Multiplatform example

commonMain định nghĩa CmpClient và NativeConsentPresenter, không port lại consent
rules. Android inject AndroidConsentPresenter gọi Maven mobile-bridge. iOS host
inject IOSConsentPresenter gọi Objective C facade trong CMPMobileBridge.

Android: dùng Gradle wrapper ở ../../android với `-p . :androidApp:assembleDebug`,
đặt CMP_MAVEN_REPO và CMP_BRIDGE_MAVEN_REPO tới các artifact repository.
iOS: trên Mac build shared framework, thêm SharedCmp và CMPMobileBridge vào host
SwiftUI, đưa iosApp/CMPKmpApp.swift vào target, thiết lập framework search paths.
Ví dụ là native modal; không có composable consent dùng chung trên hai nền tảng.

Chưa xác nhận build KMP/iOS. Không công bố tương thích trước khi chạy các host.
