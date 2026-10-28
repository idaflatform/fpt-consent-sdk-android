package vn.fpt.fis.cmp.consent;

import java.util.Locale;

import vn.fpt.fis.cmp.consent.model.ConsentField;

/**
 * Quy tac doi chieu truong cua form app voi truong trong {@code /config}: khop theo
 * {@code name}, va khop them {@code dataType} khi ca hai ben deu khai bao.
 */
public final class FieldMatcher {

    private FieldMatcher() {
    }

    /** So khop mot cap (name, dataType) cua form app voi mot truong trong config. */
    public static boolean matches(ConsentField field, String name, String dataType) {
        if (field == null || field.name == null || name == null) {
            return false;
        }
        if (!normalize(field.name).equals(normalize(name))) {
            return false;
        }
        // dataType chi dung de phan biet khi ca hai ben cung khai bao.
        if (isBlank(dataType) || isBlank(field.dataType)) {
            return true;
        }
        return normalize(field.dataType).equals(normalize(dataType));
    }

    static String compositeKey(String name, String dataType) {
        return normalize(name) + "|" + (isBlank(dataType) ? "" : normalize(dataType));
    }

    static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.US);
    }

    static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
