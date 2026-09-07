package vn.fpt.fis.cmp.consent;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Luu cuc bo: visitor ID, quyet dinh consent gan nhat va ban cache cua /config.
 *
 * <p>Visitor ID duoc sinh mot lan va gui kem moi lan submit (SRS DSAR PL 9.3.1) de lien ket
 * cac ban ghi cua cung mot thiet bi.</p>
 */
public final class ConsentStore {

    private static final String PREFS = "vn.fpt.fis.cmp.consent";
    private static final String KEY_VISITOR_ID = "visitor_id";
    private static final String KEY_STATE = "state";
    private static final String KEY_CONFIG_JSON = "config_json";
    private static final String KEY_CONFIG_AT = "config_at";

    private final SharedPreferences prefs;

    public ConsentStore(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Visitor ID on dinh cho thiet bi; sinh moi o lan goi dau tien. */
    public synchronized String visitorId() {
        String id = prefs.getString(KEY_VISITOR_ID, null);
        if (id == null) {
            id = UUID.randomUUID().toString();
            prefs.edit().putString(KEY_VISITOR_ID, id).apply();
        }
        return id;
    }

    public boolean hasConsented() {
        return prefs.contains(KEY_STATE);
    }

    public void saveState(ConsentState state) {
        if (state == null) {
            prefs.edit().remove(KEY_STATE).apply();
            return;
        }
        try {
            JSONObject json = new JSONObject();
            json.put("consentId", state.getConsentId());
            json.put("configCode", state.getConfigCode());
            json.put("submittedAt", state.getSubmittedAtMs());
            json.put("purposes", new JSONObject(state.purposes()));
            json.put("extras", new JSONObject(state.extras()));

            JSONObject fields = new JSONObject();
            for (Map.Entry<String, Map<String, ConsentState.FieldDecision>> group : state.fields().entrySet()) {
                JSONObject groupJson = new JSONObject();
                for (Map.Entry<String, ConsentState.FieldDecision> field : group.getValue().entrySet()) {
                    JSONObject decision = new JSONObject();
                    decision.put(ConsentState.KEY_IS_ACCEPT, field.getValue().accepted);
                    if (field.getValue().value != null) {
                        decision.put(ConsentState.KEY_VALUE, field.getValue().value);
                    }
                    groupJson.put(field.getKey(), decision);
                }
                fields.put(group.getKey(), groupJson);
            }
            json.put("fields", fields);
            prefs.edit().putString(KEY_STATE, json.toString()).apply();
        } catch (JSONException e) {
            prefs.edit().remove(KEY_STATE).apply();
        }
    }

    /** Quyet dinh gan nhat da submit; null neu nguoi dung chua tra loi lan nao. */
    public ConsentState loadState() {
        String raw = prefs.getString(KEY_STATE, null);
        if (raw == null) {
            return null;
        }
        try {
            JSONObject json = new JSONObject(raw);
            ConsentState state = new ConsentState();
            state.setConsentId(json.optString("consentId", null));
            state.setConfigCode(json.optString("configCode", null));
            state.setSubmittedAtMs(json.optLong("submittedAt", 0L));

            // Doc truong truoc, muc dich sau: {@code setGranted(false)} se don sach cac truong con
            // con sot lai gia tri true tu ban ghi cu (neu co), tranh trang thai mau thuan.
            JSONObject fields = json.optJSONObject("fields");
            if (fields != null) {
                for (Iterator<String> groups = fields.keys(); groups.hasNext(); ) {
                    String purposeKey = groups.next();
                    JSONObject group = fields.optJSONObject(purposeKey);
                    if (group == null) {
                        continue;
                    }
                    for (Iterator<String> it = group.keys(); it.hasNext(); ) {
                        String fieldKey = it.next();
                        JSONObject decision = group.optJSONObject(fieldKey);
                        if (decision == null) {
                            continue;
                        }
                        state.setFieldGranted(purposeKey, fieldKey,
                                decision.optBoolean(ConsentState.KEY_IS_ACCEPT, false));
                        if (decision.has(ConsentState.KEY_VALUE)) {
                            state.setFieldValue(purposeKey, fieldKey,
                                    decision.optString(ConsentState.KEY_VALUE, null));
                        }
                    }
                }
            }

            JSONObject purposes = json.optJSONObject("purposes");
            if (purposes != null) {
                for (Iterator<String> it = purposes.keys(); it.hasNext(); ) {
                    String key = it.next();
                    state.setGranted(key, purposes.optBoolean(key, false));
                }
            }

            JSONObject extras = json.optJSONObject("extras");
            if (extras != null) {
                for (Iterator<String> it = extras.keys(); it.hasNext(); ) {
                    String key = it.next();
                    state.putExtra(key, extras.opt(key));
                }
            }
            return state;
        } catch (JSONException e) {
            return null;
        }
    }

    public void saveConfig(String rawJson, long atMs) {
        prefs.edit().putString(KEY_CONFIG_JSON, rawJson).putLong(KEY_CONFIG_AT, atMs).apply();
    }

    /** Config da cache neu con han theo {@code ttlMs}; null khi khong co hoac het han. */
    public String loadConfig(long ttlMs, long nowMs) {
        String raw = prefs.getString(KEY_CONFIG_JSON, null);
        long at = prefs.getLong(KEY_CONFIG_AT, 0L);
        if (raw == null || at <= 0 || nowMs - at > ttlMs) {
            return null;
        }
        return raw;
    }

    /** Config da cache bat ke con han hay khong — chi dung lam phuong an du phong khi mat mang. */
    public String loadConfigIgnoringTtl() {
        return prefs.getString(KEY_CONFIG_JSON, null);
    }

    /** Xoa rieng ban cache cua /config; giu quyet dinh da luu va visitor ID. */
    public void clearConfigCache() {
        prefs.edit().remove(KEY_CONFIG_JSON).remove(KEY_CONFIG_AT).apply();
    }

    /** Xoa quyet dinh + cache; giu lai visitor ID. */
    public void clear() {
        prefs.edit().remove(KEY_STATE).remove(KEY_CONFIG_JSON).remove(KEY_CONFIG_AT).apply();
    }
}
