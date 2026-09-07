package vn.fpt.fis.cmp.consent.model;

import java.io.Serializable;

/** Mot truong du lieu duoc thu thap trong pham vi mot purpose (vd Ho va ten, Email, So dien thoai). */
public final class ConsentField implements Serializable {

    public final String id;
    /** Ten ky thuat — dung ghep khoa khi submit. */
    public final String name;
    /** Nhan hien thi cho nguoi dung. */
    public final String title;
    public final String dataType;
    public final boolean required;
    public final boolean sensitive;
    /** True khi truong nay duoc chia se cho he thong / ben thu ba. */
    public final boolean sharedWithSystem;

    public ConsentField(String id, String name, String title, String dataType,
                        boolean required, boolean sensitive, boolean sharedWithSystem) {
        this.id = id;
        this.name = name;
        this.title = title;
        this.dataType = dataType;
        this.required = required;
        this.sensitive = sensitive;
        this.sharedWithSystem = sharedWithSystem;
    }

    /** Nhan hien thi: uu tien {@code title}, fallback {@code name}. */
    public String displayName() {
        return title != null && title.length() > 0 ? title : name;
    }

    /** Khoa gui len /sendData — la {@code id} cua truong (fallback {@code name} khi thieu id). */
    public String key() {
        return id != null ? id : name;
    }

    /** True khi truong nay du dieu kien lay gia tri tu form cua app de gui kem lam bang chung. */
    public boolean collectsValue() {
        return sharedWithSystem;
    }
}
