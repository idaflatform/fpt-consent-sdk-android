package vn.fpt.fis.cmp.consent.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/** Khoi {@code config} trong response /config: tieu de, mo ta, nhan nut, danh sach purpose. */
public final class ConsentUiConfig implements Serializable {

    public final String title;
    public final String description;
    public final String submitLabel;
    public final List<ConsentItem> items;

    public ConsentUiConfig(String title, String description, String submitLabel, List<ConsentItem> items) {
        this.title = title;
        this.description = description;
        this.submitLabel = submitLabel;
        this.items = items == null ? Collections.<ConsentItem>emptyList() : items;
    }
}
