# Unity integration

Import package bằng đường dẫn, import sample. Tạo GameObject có CmpConsent,
gắn CmpExample và tham chiếu consent. Sample dựng UI bằng OnGUI nên không cần
scene hoặc prefab phụ thuộc project cụ thể.

Android: thêm Maven mobile-bridge 0.1.0 và repository SDK trong Gradle template.
Thư viện cần AndroidX và Material; không chỉ copy riêng một AAR rồi bỏ dependency.
iOS: link/embed CMPMobileBridge và ConsentCmp framework trong exported Xcode
project; CmpConsent.mm gọi facade Objective C. Callback được chuyển về Update,
không gọi Unity API từ Android UI thread. Giữ GameObject sống khi màn native mở.

Chưa build bằng Unity Editor hoặc IL2CPP. Cần nghiệm thu trên cả hai nền tảng;
mã hiện tại chỉ hỗ trợ native modal, không render form vào Unity canvas.
