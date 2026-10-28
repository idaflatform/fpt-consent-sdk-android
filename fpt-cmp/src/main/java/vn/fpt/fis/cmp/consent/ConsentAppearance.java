package vn.fpt.fis.cmp.consent;

import android.graphics.Color;
import org.json.JSONObject;
import vn.fpt.fis.cmp.consent.api.ConsentJson;

/** Immutable, versioned native appearance. Missing/invalid tokens use SDK defaults. */
public final class ConsentAppearance {
    private final String raw;
    public final String revision;
    private ConsentAppearance(JSONObject json) {
        raw = json.toString(); revision = json.optString("revision", "default");
    }
    public static ConsentAppearance defaults() { return new ConsentAppearance(new JSONObject()); }
    public static ConsentAppearance parse(JSONObject response) throws Exception {
        JSONObject json = ConsentJson.unwrap(response);
        if (json.optInt("schemaVersion") != 1 || json.optJSONObject("theme") == null
                || json.optJSONObject("typography") == null || json.toString().length() > 32768)
            throw new IllegalArgumentException("Unsupported appearance");
        return new ConsentAppearance(json);
    }
    public JSONObject json() {
        try { return new JSONObject(raw); } catch (Exception ignored) { return new JSONObject(); }
    }
    public int color(String token, int fallback) {
        JSONObject theme = json().optJSONObject("theme");
        JSONObject colors = theme == null ? null : theme.optJSONObject("colors");
        String value = colors == null ? "" : colors.optString(token);
        if (!value.matches("#[0-9a-fA-F]{6}")) return fallback;
        return Color.parseColor(value);
    }
    public float radius(String token, float fallback) {
        JSONObject theme = json().optJSONObject("theme");
        JSONObject shape = theme == null ? null : theme.optJSONObject("shape");
        return number(shape, token, fallback, 0, 32);
    }
    public String family() {
        JSONObject type = json().optJSONObject("typography");
        return type == null ? "system" : type.optString("family", "system");
    }
    public float size(String role, float fallback) { return style(role, "size", fallback, 10, 32); }
    public int weight(String role, int fallback) { return (int) style(role, "weight", fallback, 100, 900); }
    private float style(String role, String token, float fallback, float min, float max) {
        JSONObject type = json().optJSONObject("typography");
        return number(type == null ? null : type.optJSONObject(role), token, fallback, min, max);
    }
    private static float number(JSONObject node, String token, float fallback, float min, float max) {
        if (node == null || !(node.opt(token) instanceof Number)) return fallback;
        double value = node.optDouble(token, fallback);
        return Double.isNaN(value) || Double.isInfinite(value) || value < min || value > max ? fallback : (float) value;
    }
}
