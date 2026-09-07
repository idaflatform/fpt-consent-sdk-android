package vn.fpt.fis.cmp.consent.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Response cua GET {baseUrl}/api/v1/cmp/consent/config?code_config=...
 *
 * <p>Map 1-1 voi {@code ConsentConfigResponse} phia backend (fis-cmp).</p>
 */
public final class ConsentConfig implements Serializable {

    /** Ma form / integration key cua Collection Point. */
    public final String code;
    /** Ten form hien thi. */
    public final String name;
    /** {@code active} / {@code inactive} — chi form active moi dung duoc. */
    public final String status;
    /** Cac truong nhap lieu cua form (luong form demo); rong o luong Data Consent. */
    public final List<FormField> fields;
    /** Cau hinh UI khoi consent. */
    public final ConsentUiConfig config;

    public ConsentConfig(String code, String name, String status,
                         List<FormField> fields, ConsentUiConfig config) {
        this.code = code;
        this.name = name;
        this.status = status;
        this.fields = fields == null ? Collections.<FormField>emptyList() : fields;
        this.config = config;
    }

    /** True khi form dang active (chi khi do moi nen hien UI consent). */
    public boolean isActive() {
        return status == null || "active".equalsIgnoreCase(status);
    }

    /** Danh sach purpose hien thi; khong bao gio null. */
    public List<ConsentItem> items() {
        if (config == null || config.items == null) {
            return Collections.emptyList();
        }
        return config.items;
    }
}
