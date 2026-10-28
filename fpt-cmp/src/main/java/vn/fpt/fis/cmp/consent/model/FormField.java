package vn.fpt.fis.cmp.consent.model;

import java.io.Serializable;

/**
 * Truong nhap lieu cua form (luong form demo cua backend: {@code cmp_form_fields}).
 * Gia tri nguoi dung nhap duoc gui kem trong {@code values} khi submit.
 */
public final class FormField implements Serializable {

    public final String key;
    public final String label;
    public final String resourceUse;
    /** text / email / tel ... */
    public final String typeInput;

    public FormField(String key, String label, String resourceUse, String typeInput) {
        this.key = key;
        this.label = label;
        this.resourceUse = resourceUse;
        this.typeInput = typeInput;
    }
}
