package vn.fpt.fis.cmp.consent.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Mot muc dich xu ly du lieu (purpose) trong danh sach su dong y.
 *
 * <p>{@link #code} la khoa gui len /sendData trong object {@code values}
 * (o luong Data Consent, code = {@code purpose_version_id}).</p>
 */
public final class ConsentItem implements Serializable {

    public final String id;
    public final String code;
    public final String label;
    public final String description;
    public final boolean required;
    public final boolean defaultChecked;
    /** Version purpose da ghim, vd {@code v1.0}. */
    public final String version;
    /** Co so phap ly cua purpose. */
    public final String legalBasis;
    /** Loai du lieu lien quan (tuong thich cu). */
    public final List<DataTypeItem> dataTypes;
    /** Truong du lieu thu thap — moi truong la mot dong co toggle rieng trong UI. */
    public final List<ConsentField> dataFields;
    /** Ben thu ba / he thong duoc chia se du lieu cua purpose nay. */
    public final List<String> thirdParties;

    public ConsentItem(String id, String code, String label, String description,
                       boolean required, boolean defaultChecked, String version, String legalBasis,
                       List<DataTypeItem> dataTypes, List<ConsentField> dataFields, List<String> thirdParties) {
        this.id = id;
        this.code = code != null ? code : id;
        this.label = label;
        this.description = description;
        this.required = required;
        this.defaultChecked = defaultChecked || required;
        this.version = version;
        this.legalBasis = legalBasis;
        this.dataTypes = dataTypes == null ? Collections.<DataTypeItem>emptyList() : dataTypes;
        this.dataFields = dataFields == null ? Collections.<ConsentField>emptyList() : dataFields;
        this.thirdParties = thirdParties == null ? Collections.<String>emptyList() : thirdParties;
    }

    /** Khoa on dinh dung lam key trong {@code values} khi submit. */
    public String key() {
        return code != null ? code : id;
    }

    /**
     * Truong du lieu bat buoc dau tien cua purpose nay; null neu khong co.
     *
     * <p>Chi tinh truong dang hien: truong {@code display = false} nguoi dung khong nhin thay va
     * khong bam duoc, nen khong duoc phep chan submit hay khoa purpose (R16).</p>
     */
    public ConsentField firstRequiredField() {
        for (ConsentField field : dataFields) {
            if (field.required && field.display) {
                return field;
            }
        }
        return null;
    }

    public boolean hasRequiredField() {
        return firstRequiredField() != null;
    }

    /**
     * True khi purpose nay khong the tat.
     *
     * <p>Gom 2 truong hop: chinh purpose {@code required = true}, hoac purpose chua truong du lieu
     * {@code required = true} — tat purpose se keo truong bat buoc tat theo, nen phai chan.</p>
     */
    public boolean mustBeGranted() {
        return required || hasRequiredField();
    }
}
