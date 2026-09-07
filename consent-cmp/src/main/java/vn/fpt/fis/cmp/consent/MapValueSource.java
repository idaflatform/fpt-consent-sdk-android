package vn.fpt.fis.cmp.consent;

import androidx.annotation.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

import vn.fpt.fis.cmp.consent.model.ConsentField;

/**
 * Nguon gia tri khai bao tay: app tu dat gia tri theo {@code name} (va {@code dataType} neu can
 * phan biet hai truong trung ten).
 *
 * <pre>{@code
 * MapValueSource values = new MapValueSource()
 *         .put("full_name", binding.edtName.getText().toString())
 *         .put("email", "EMAIL", binding.edtEmail.getText().toString());
 * ConsentCmp.get().setValueSource(values);
 * }</pre>
 */
public final class MapValueSource implements ConsentValueSource {

    private final Map<String, String> byNameAndType = new LinkedHashMap<>();
    private final Map<String, String> byName = new LinkedHashMap<>();

    public MapValueSource put(String name, String value) {
        if (name != null) {
            byName.put(FieldMatcher.normalize(name), value);
        }
        return this;
    }

    public MapValueSource put(String name, String dataType, String value) {
        if (name != null) {
            byNameAndType.put(FieldMatcher.compositeKey(name, dataType), value);
        }
        return this;
    }

    @Nullable
    @Override
    public String valueFor(ConsentField field) {
        if (field == null || field.name == null) {
            return null;
        }
        // Uu tien khop ca ten lan kieu du lieu, sau do moi khop theo ten.
        String value = byNameAndType.get(FieldMatcher.compositeKey(field.name, field.dataType));
        return value != null ? value : byName.get(FieldMatcher.normalize(field.name));
    }
}
