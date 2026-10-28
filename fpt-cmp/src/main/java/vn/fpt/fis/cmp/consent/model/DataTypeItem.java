package vn.fpt.fis.cmp.consent.model;

import java.io.Serializable;

/** Loai du lieu gan voi purpose (giu tuong thich voi payload cu cua /config). */
public final class DataTypeItem implements Serializable {

    public final String id;
    public final String name;
    public final String title;
    public final boolean sensitive;

    public DataTypeItem(String id, String name, String title, boolean sensitive) {
        this.id = id;
        this.name = name;
        this.title = title;
        this.sensitive = sensitive;
    }
}
