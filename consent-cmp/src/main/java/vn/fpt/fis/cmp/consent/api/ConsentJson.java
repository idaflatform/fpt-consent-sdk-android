package vn.fpt.fis.cmp.consent.api;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import vn.fpt.fis.cmp.consent.model.ConsentConfig;
import vn.fpt.fis.cmp.consent.model.ConsentField;
import vn.fpt.fis.cmp.consent.model.ConsentItem;
import vn.fpt.fis.cmp.consent.model.ConsentUiConfig;
import vn.fpt.fis.cmp.consent.model.DataTypeItem;
import vn.fpt.fis.cmp.consent.model.FormField;
import vn.fpt.fis.cmp.consent.model.SendConsentResult;

/** Doc/ghi JSON cho 2 API consent (dung org.json co san trong Android, khong them dependency). */
public final class ConsentJson {

    private ConsentJson() {
    }

    /** Boc/bo lop {@code BaseResponse}: tra ve {@code data} neu co, nguoc lai chinh object goc. */
    public static JSONObject unwrap(JSONObject root) {
        JSONObject data = root.optJSONObject("data");
        return data != null ? data : root;
    }

    public static ConsentConfig parseConfig(JSONObject json) {
        JSONObject data = unwrap(json);
        return new ConsentConfig(
                optString(data, "code"),
                optString(data, "name"),
                optString(data, "status"),
                parseFormFields(data.optJSONArray("fields")),
                parseUiConfig(data.optJSONObject("config")));
    }

    public static SendConsentResult parseSendResult(JSONObject json) {
        JSONObject data = unwrap(json);
        return new SendConsentResult(
                data.optBoolean("ok", true),
                optString(data, "consentId"),
                optString(data, "code"),
                optString(data, "dateCreated"));
    }

    private static ConsentUiConfig parseUiConfig(JSONObject json) {
        if (json == null) {
            return new ConsentUiConfig(null, null, null, null);
        }
        return new ConsentUiConfig(
                optString(json, "title"),
                optString(json, "description"),
                optString(json, "submitLabel"),
                parseItems(json.optJSONArray("items")));
    }

    private static List<ConsentItem> parseItems(JSONArray array) {
        List<ConsentItem> items = new ArrayList<>();
        if (array == null) {
            return items;
        }
        for (int i = 0; i < array.length(); i++) {
            JSONObject o = array.optJSONObject(i);
            if (o == null) {
                continue;
            }
            items.add(new ConsentItem(
                    optString(o, "id"),
                    optString(o, "code"),
                    optString(o, "label"),
                    optString(o, "description"),
                    o.optBoolean("required", false),
                    o.optBoolean("defaultChecked", false),
                    optString(o, "version"),
                    optString(o, "legalBasis"),
                    parseDataTypes(o.optJSONArray("dataTypes")),
                    parseDataFields(o.optJSONArray("dataFields")),
                    parseStrings(o.optJSONArray("thirdParties"))));
        }
        return items;
    }

    private static List<ConsentField> parseDataFields(JSONArray array) {
        List<ConsentField> fields = new ArrayList<>();
        if (array == null) {
            return fields;
        }
        for (int i = 0; i < array.length(); i++) {
            JSONObject o = array.optJSONObject(i);
            if (o == null) {
                continue;
            }
            fields.add(new ConsentField(
                    optString(o, "id"),
                    optString(o, "name"),
                    optString(o, "title"),
                    optString(o, "dataType"),
                    o.optBoolean("required", false),
                    o.optBoolean("sensitive", false),
                    o.optBoolean("sharedWithSystem", false),
                    // Backend cu khong tra "display" -> mac dinh hien, giu tuong thich nguoc (R16).
                    o.optBoolean("display", true)));
        }
        return fields;
    }

    private static List<DataTypeItem> parseDataTypes(JSONArray array) {
        List<DataTypeItem> types = new ArrayList<>();
        if (array == null) {
            return types;
        }
        for (int i = 0; i < array.length(); i++) {
            JSONObject o = array.optJSONObject(i);
            if (o == null) {
                continue;
            }
            types.add(new DataTypeItem(
                    optString(o, "id"),
                    optString(o, "name"),
                    optString(o, "title"),
                    o.optBoolean("sensitive", false)));
        }
        return types;
    }

    private static List<FormField> parseFormFields(JSONArray array) {
        List<FormField> fields = new ArrayList<>();
        if (array == null) {
            return fields;
        }
        for (int i = 0; i < array.length(); i++) {
            JSONObject o = array.optJSONObject(i);
            if (o == null) {
                continue;
            }
            fields.add(new FormField(
                    optString(o, "key"),
                    optString(o, "label"),
                    optString(o, "resourceUse"),
                    optString(o, "typeInput")));
        }
        return fields;
    }

    private static List<String> parseStrings(JSONArray array) {
        List<String> values = new ArrayList<>();
        if (array == null) {
            return values;
        }
        for (int i = 0; i < array.length(); i++) {
            String value = array.optString(i, null);
            if (value != null && !value.isEmpty()) {
                values.add(value);
            }
        }
        return values;
    }

    /** {@code null} thay vi chuoi "null" cua {@link JSONObject#optString(String)}. */
    public static String optString(JSONObject json, String key) {
        if (json == null || json.isNull(key)) {
            return null;
        }
        String value = json.optString(key, null);
        return value == null || value.isEmpty() ? null : value;
    }

    /**
     * Dung body {@code SendConsentRequest}.
     *
     * @param source nguon gui (ten app + package + version); bo qua khi null
     */
    public static JSONObject buildSendRequest(String consentId, String code, String dateCreated,
                                              String status, String visitorId, String source,
                                              Map<String, Object> values) throws JSONException {
        JSONObject valuesJson = toJson(values);
        JSONObject body = new JSONObject();
        body.put("consentId", consentId);
        body.put("code", code);
        body.put("dateCreated", dateCreated);
        body.put("status", status);
        if (visitorId != null) {
            body.put("visitorId", visitorId);
        }
        if (source != null && !source.isEmpty()) {
            body.put("source", source);
        }
        body.put("values", valuesJson);
        return body;
    }

    /** Chuyen map long nhau (purpose -&gt; truong du lieu -&gt; isAccept/value) thanh JSON. */
    @SuppressWarnings("unchecked")
    private static JSONObject toJson(Map<String, Object> values) throws JSONException {
        JSONObject json = new JSONObject();
        if (values == null) {
            return json;
        }
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            Object value = entry.getValue();
            if (value instanceof Map) {
                json.put(entry.getKey(), toJson((Map<String, Object>) value));
            } else {
                json.put(entry.getKey(), value == null ? JSONObject.NULL : value);
            }
        }
        return json;
    }
}
