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
    /**
     * Co hoi nguoi dung ve truong nay hay khong.
     *
     * <p>{@code /config} tra ve MOI truong cua form du lieu nguon, nhung chi truong duoc chon cho
     * purpose trong template moi co {@code display = true}. Truong {@code display = false} khong
     * dung UI, khong bao gio {@code isAccept = true} va khong tinh vao rang buoc bat buoc, nhung
     * VAN duoc quet gia tri tu form de gui kem khi {@code sharedWithSystem = true} (quy tac R16).
     * Backend cu khong tra khoa nay — khi do mac dinh {@code true}.</p>
     */
    public final boolean display;

    /** Tuong thich nguoc: khong khai bao {@code display} thi coi nhu truong duoc hien thi. */
    public ConsentField(String id, String name, String title, String dataType,
                        boolean required, boolean sensitive, boolean sharedWithSystem) {
        this(id, name, title, dataType, required, sensitive, sharedWithSystem, true);
    }

    public ConsentField(String id, String name, String title, String dataType,
                        boolean required, boolean sensitive, boolean sharedWithSystem,
                        boolean display) {
        this.id = id;
        this.name = name;
        this.title = title;
        this.dataType = dataType;
        this.required = required;
        this.sensitive = sensitive;
        this.sharedWithSystem = sharedWithSystem;
        this.display = display;
    }

    /** Nhan hien thi: uu tien {@code title}, fallback {@code name}. */
    public String displayName() {
        return title != null && title.length() > 0 ? title : name;
    }

    /** Khoa gui len /sendData — la {@code id} cua truong (fallback {@code name} khi thieu id). */
    public String key() {
        return id != null ? id : name;
    }

    /**
     * True khi truong nay du dieu kien lay gia tri tu form cua app de gui kem lam bang chung.
     *
     * <p>Khong phu thuoc {@code display}: truong an khong duoc hoi nguoi dung (nen luon
     * {@code isAccept = false}) nhung van phai ghi lai gia tri da chia se cho he thong (R16).</p>
     */
    public boolean collectsValue() {
        return sharedWithSystem;
    }
}
