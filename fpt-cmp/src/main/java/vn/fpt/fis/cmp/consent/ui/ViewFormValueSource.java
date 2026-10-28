package vn.fpt.fis.cmp.consent.ui;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;

import vn.fpt.fis.cmp.consent.ConsentValueSource;
import vn.fpt.fis.cmp.consent.FieldMatcher;
import vn.fpt.fis.cmp.consent.model.ConsentField;

/**
 * Quet form cua app de lay gia tri nguoi dung dang nhap, doi chieu voi truong trong {@code /config}
 * theo {@code name} (+ {@code dataType} neu co).
 *
 * <p>App danh dau input bang {@code android:tag}:</p>
 *
 * <pre>{@code
 * <EditText android:id="@+id/edtEmail" android:tag="email|EMAIL" />
 * <EditText android:id="@+id/edtName" android:tag="full_name" />
 * }</pre>
 *
 * <p>Cu phap tag: {@code name} hoac {@code name|dataType} (chap nhan ca {@code name:dataType}).
 * Quet lai moi lan lay gia tri nen luon doc duoc noi dung moi nhat cua o nhap.</p>
 */
public final class ViewFormValueSource implements ConsentValueSource {

    private final WeakReference<View> rootRef;

    public ViewFormValueSource(View formRoot) {
        this.rootRef = new WeakReference<>(formRoot);
    }

    @Nullable
    @Override
    public String valueFor(ConsentField field) {
        View root = rootRef.get();
        if (root == null || field == null) {
            return null;
        }
        return find(root, field);
    }

    @Nullable
    private String find(View view, ConsentField field) {
        Object tag = view.getTag();
        if (tag instanceof CharSequence && view instanceof TextView) {
            String[] parts = split(tag.toString());
            if (FieldMatcher.matches(field, parts[0], parts[1])) {
                CharSequence text = ((TextView) view).getText();
                String value = text == null ? null : text.toString().trim();
                return value == null || value.isEmpty() ? null : value;
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                String value = find(group.getChildAt(i), field);
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }

    /** Tach tag thanh {@code [name, dataType]}; dataType co the null. */
    private String[] split(String tag) {
        int index = tag.indexOf('|');
        if (index < 0) {
            index = tag.indexOf(':');
        }
        if (index < 0) {
            return new String[]{tag, null};
        }
        return new String[]{tag.substring(0, index), tag.substring(index + 1)};
    }
}
