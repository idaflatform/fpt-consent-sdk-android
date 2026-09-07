package vn.fpt.fis.cmp.consent;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import vn.fpt.fis.cmp.consent.model.ConsentConfig;
import vn.fpt.fis.cmp.consent.model.ConsentField;
import vn.fpt.fis.cmp.consent.model.ConsentItem;

/**
 * Quyet dinh cua nguoi dung: theo purpose va theo tung truong du lieu trong purpose.
 *
 * <p>Khi submit, moi purpose la mot object long trong {@code values}:</p>
 *
 * <pre>{@code
 * "019f1177-2aca-7428-99b0-9abe5c3bca52": {
 *   "isAccept": true,
 *   "019f5fcc-c415-7140-ae99-534643c3c096": { "value": "Le thanh trung", "isAccept": true },
 *   "019f5fcc-c428-7e01-a34c-963dc70688cf": { "value": "a@yopmail.com", "isAccept": false },
 *   "019f5fcc-c42f-722d-b5e7-fbc099a287df": { "isAccept": true }
 * }
 * }</pre>
 *
 * <p>Khoa purpose = {@code item.code}, khoa truong = {@code field.id}. Truong nao khong lay duoc
 * gia tri tu form cua app thi chi co {@code isAccept} (nhu phan tu thu ba o vi du tren).</p>
 */
public final class ConsentState implements Serializable {

    /** Ten khoa co dinh trong object cua purpose / truong du lieu. */
    public static final String KEY_IS_ACCEPT = "isAccept";
    public static final String KEY_VALUE = "value";

    /** Quyet dinh cho mot truong du lieu: dong y hay khong, kem gia tri neu form co truong nay. */
    public static final class FieldDecision implements Serializable {

        public boolean accepted;
        /** Gia tri nguoi dung nhap; null khi form cua app khong co truong tuong ung. */
        public String value;

        FieldDecision(boolean accepted, String value) {
            this.accepted = accepted;
            this.value = value;
        }
    }

    private final Map<String, Boolean> purposes = new LinkedHashMap<>();
    private final Map<String, Map<String, FieldDecision>> fields = new LinkedHashMap<>();
    private final Map<String, Object> extras = new LinkedHashMap<>();

    private String consentId;
    private String configCode;
    private long submittedAtMs;

    /** Trang thai mac dinh suy ra tu config: purpose bat buoc = bat, con lai theo {@code defaultChecked}. */
    public static ConsentState defaultsOf(ConsentConfig config) {
        ConsentState state = new ConsentState();
        if (config == null) {
            return state;
        }
        state.configCode = config.code;
        for (ConsentItem item : config.items()) {
            // Chi muc/truong co required = true (hoac purpose defaultChecked) moi bat san khi mo man
            // hinh; phan con lai de tat cho nguoi dung tu chon.
            boolean granted = item.mustBeGranted() || item.defaultChecked;
            state.setGranted(item.key(), granted);
            for (ConsentField field : item.dataFields) {
                // Purpose cha phai dang bat moi giu duoc truong bat buoc o trang thai bat.
                state.setFieldGranted(item.key(), field.key(), granted && field.required);
            }
        }
        return state;
    }

    /**
     * Dung trang thai khop dung cau hinh hien tai, giu lai lua chon cu neu con hop le.
     *
     * <p>Dung khi mo lai man hinh consent: cau hinh co the da doi (them/bo purpose, doi truong du
     * lieu). Ket qua chi chua khoa co trong {@code config} — lua chon cu cua khoa da bi go se khong
     * lot vao request moi; purpose/truong moi lay gia tri mac dinh.</p>
     *
     * @param saved trang thai da luu; null thi tra ve mac dinh cua config
     */
    public static ConsentState merge(ConsentConfig config, ConsentState saved) {
        ConsentState state = defaultsOf(config);
        if (config == null || saved == null) {
            return state;
        }
        for (ConsentItem item : config.items()) {
            String purposeKey = item.key();
            Boolean savedPurpose = saved.purposes.get(purposeKey);
            if (savedPurpose != null) {
                state.setGranted(purposeKey, savedPurpose || item.mustBeGranted());
            }
            for (ConsentField field : item.dataFields) {
                FieldDecision savedField = saved.decision(purposeKey, field.key(), false);
                if (savedField == null) {
                    continue;
                }
                // Truong bat buoc: khi purpose dang bat thi luon bat, khong lay lua chon cu.
                boolean granted = state.isGranted(purposeKey)
                        && (field.required || savedField.accepted);
                state.setFieldGranted(purposeKey, field.key(), granted);
                state.setFieldValue(purposeKey, field.key(), savedField.value);
            }
        }
        return state;
    }

    /** Bo cac purpose / truong khong con trong cau hinh hien tai (khong them khoa moi). */
    public void retainOnly(ConsentConfig config) {
        if (config == null) {
            return;
        }
        Map<String, Set<String>> allowed = new LinkedHashMap<>();
        for (ConsentItem item : config.items()) {
            Set<String> fieldKeys = new LinkedHashSet<>();
            for (ConsentField field : item.dataFields) {
                fieldKeys.add(field.key());
            }
            allowed.put(item.key(), fieldKeys);
        }
        purposes.keySet().retainAll(allowed.keySet());
        fields.keySet().retainAll(allowed.keySet());
        for (Map.Entry<String, Map<String, FieldDecision>> group : fields.entrySet()) {
            group.getValue().keySet().retainAll(allowed.get(group.getKey()));
        }
    }

    public boolean isGranted(String purposeKey) {
        Boolean value = purposes.get(purposeKey);
        return value != null && value;
    }

    public boolean isFieldGranted(String purposeKey, String fieldKey) {
        FieldDecision decision = decision(purposeKey, fieldKey, false);
        return decision != null && decision.accepted;
    }

    public String fieldValue(String purposeKey, String fieldKey) {
        FieldDecision decision = decision(purposeKey, fieldKey, false);
        return decision != null ? decision.value : null;
    }

    /**
     * Bat/tat mot muc dich.
     *
     * <p>Tat muc dich thi moi truong du lieu ben trong cung tat theo — bat buoc phai giu bat bien nay
     * o tang state, neu khong payload co the ra tinh huong mau thuan: purpose {@code isAccept = false}
     * nhung truong con van {@code isAccept = true}.</p>
     */
    public void setGranted(String purposeKey, boolean granted) {
        purposes.put(purposeKey, granted);
        if (!granted) {
            Map<String, FieldDecision> group = fields.get(purposeKey);
            if (group != null) {
                for (FieldDecision decision : group.values()) {
                    decision.accepted = false;
                }
            }
        }
    }

    /** Bat/tat mot truong du lieu; bat truong thi muc dich cha cung duoc bat theo. */
    public void setFieldGranted(String purposeKey, String fieldKey, boolean granted) {
        decision(purposeKey, fieldKey, true).accepted = granted;
        if (granted) {
            purposes.put(purposeKey, true);
        }
    }

    /** Gan gia tri nguoi dung nhap cho mot truong; {@code null} = khong gui khoa {@code value}. */
    public void setFieldValue(String purposeKey, String fieldKey, String value) {
        decision(purposeKey, fieldKey, true).value = value;
    }

    // ==================== Thao tac cua nguoi dung (quy tac dung chung moi platform) ====================

    /** Ket qua ap dung mot thao tac toggle. */
    public enum ToggleResult {
        /** Da ap dung dung y nguoi dung. */
        APPLIED,
        /** Bi chan vi muc/truong bat buoc — trang thai da duoc dua ve muc toi thieu. */
        BLOCKED_REQUIRED
    }

    /**
     * Nguoi dung bat/tat mot muc dich.
     *
     * <p>Bat: bat luon moi truong du lieu ben trong. Tat: tat het truong; nhung neu muc dich khong
     * the tat ({@link ConsentItem#mustBeGranted()}) thi dua ve trang thai toi thieu va tra
     * {@link ToggleResult#BLOCKED_REQUIRED} de UI hien ly do.</p>
     */
    public ToggleResult applyPurposeToggle(ConsentConfig config, String purposeKey, boolean granted) {
        ConsentItem item = itemOf(config, purposeKey);
        if (item == null) {
            setGranted(purposeKey, granted);
            return ToggleResult.APPLIED;
        }
        if (!granted && item.mustBeGranted()) {
            setMinimumRequired(config, purposeKey);
            return ToggleResult.BLOCKED_REQUIRED;
        }
        setGranted(purposeKey, granted);
        if (granted) {
            for (ConsentField field : item.dataFields) {
                setFieldGranted(purposeKey, field.key(), true);
            }
        }
        return ToggleResult.APPLIED;
    }

    /**
     * Nguoi dung bat/tat mot truong du lieu.
     *
     * <p>Bat: muc dich cha bat theo. Tat: neu truong {@code required = true} va muc dich dang bat thi
     * bi chan; neu tat het truong va muc dich co the tat thi muc dich tat theo.</p>
     */
    public ToggleResult applyFieldToggle(ConsentConfig config, String purposeKey, String fieldKey,
                                         boolean granted) {
        ConsentItem item = itemOf(config, purposeKey);
        ConsentField field = fieldOf(item, fieldKey);

        if (!granted && field != null && field.required && isGranted(purposeKey)) {
            setFieldGranted(purposeKey, fieldKey, true);
            return ToggleResult.BLOCKED_REQUIRED;
        }
        setFieldGranted(purposeKey, fieldKey, granted);
        if (!granted && item != null && !item.mustBeGranted() && !anyFieldGranted(item)) {
            setGranted(purposeKey, false);
        }
        return ToggleResult.APPLIED;
    }

    /** Nguoi dung bam "Dong y tat ca": bat moi muc dich va moi truong du lieu. */
    public void applyAcceptAll(ConsentConfig config) {
        if (config == null) {
            return;
        }
        for (ConsentItem item : config.items()) {
            setGranted(item.key(), true);
            for (ConsentField field : item.dataFields) {
                setFieldGranted(item.key(), field.key(), true);
            }
        }
    }

    /**
     * Nguoi dung bam "Tu choi tat ca": tat moi thu, tru cac muc dich khong the tat — chung ve trang
     * thai toi thieu. Tra {@link ToggleResult#BLOCKED_REQUIRED} khi co it nhat mot muc nhu vay.
     */
    public ToggleResult applyRejectAll(ConsentConfig config) {
        if (config == null) {
            return ToggleResult.APPLIED;
        }
        ToggleResult result = ToggleResult.APPLIED;
        for (ConsentItem item : config.items()) {
            if (item.mustBeGranted()) {
                setMinimumRequired(config, item.key());
                result = ToggleResult.BLOCKED_REQUIRED;
            } else {
                setGranted(item.key(), false);
            }
        }
        return result;
    }

    /** Trang thai toi thieu cua mot muc dich: muc dich bat, chi truong bat buoc bat. */
    public void setMinimumRequired(ConsentConfig config, String purposeKey) {
        ConsentItem item = itemOf(config, purposeKey);
        if (item == null) {
            return;
        }
        setGranted(purposeKey, true);
        for (ConsentField field : item.dataFields) {
            setFieldGranted(purposeKey, field.key(), field.required);
        }
    }

    private boolean anyFieldGranted(ConsentItem item) {
        for (ConsentField field : item.dataFields) {
            if (isFieldGranted(item.key(), field.key())) {
                return true;
            }
        }
        return false;
    }

    private static ConsentItem itemOf(ConsentConfig config, String purposeKey) {
        if (config == null || purposeKey == null) {
            return null;
        }
        for (ConsentItem item : config.items()) {
            if (purposeKey.equals(item.key())) {
                return item;
            }
        }
        return null;
    }

    private static ConsentField fieldOf(ConsentItem item, String fieldKey) {
        if (item == null || fieldKey == null) {
            return null;
        }
        for (ConsentField field : item.dataFields) {
            if (fieldKey.equals(field.key())) {
                return field;
            }
        }
        return null;
    }

    /** Gia tri bo sung gui o cap ngoai cung cua {@code values} (metadata / field form demo). */
    public void putExtra(String key, Object value) {
        extras.put(key, value);
    }

    public Map<String, Boolean> purposes() {
        return Collections.unmodifiableMap(purposes);
    }

    public Map<String, Map<String, FieldDecision>> fields() {
        return Collections.unmodifiableMap(fields);
    }

    public Map<String, Object> extras() {
        return Collections.unmodifiableMap(extras);
    }

    /** True khi tat ca purpose va truong con deu bat — dung cho toggle "Dong y tat ca". */
    public boolean isAllGranted() {
        if (purposes.isEmpty()) {
            return false;
        }
        for (Boolean value : purposes.values()) {
            if (value == null || !value) {
                return false;
            }
        }
        for (Map<String, FieldDecision> group : fields.values()) {
            for (FieldDecision decision : group.values()) {
                if (!decision.accepted) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Purpose bat buoc dau tien chua duoc dong y; null khi hop le. */
    public ConsentItem firstMissingRequired(ConsentConfig config) {
        MissingRequired missing = firstMissing(config);
        return missing != null && missing.field == null ? missing.item : null;
    }

    /** Mot muc bat buoc con thieu: hoac chinh purpose, hoac mot truong du lieu cua purpose do. */
    public static final class MissingRequired implements Serializable {

        /** Purpose lien quan; luon co. */
        public final ConsentItem item;
        /** Truong du lieu bat buoc con thieu; null khi thieu chinh purpose. */
        public final ConsentField field;

        MissingRequired(ConsentItem item, ConsentField field) {
            this.item = item;
            this.field = field;
        }
    }

    /**
     * Muc bat buoc dau tien chua duoc dong y; null khi du dieu kien submit.
     *
     * <p>Kiem tra 2 loai: purpose {@code required = true} chua bat, va truong du lieu
     * {@code required = true} chua bat trong mot purpose <b>da bat</b> — truong bat buoc chi co nghia
     * khi nguoi dung dong y purpose chua no.</p>
     */
    public MissingRequired firstMissing(ConsentConfig config) {
        if (config == null) {
            return null;
        }
        for (ConsentItem item : config.items()) {
            boolean granted = isGranted(item.key());
            if (!granted) {
                // Purpose bat buoc, hoac purpose dang chua truong bat buoc, thi khong duoc tat.
                if (item.required) {
                    return new MissingRequired(item, null);
                }
                ConsentField requiredField = item.firstRequiredField();
                if (requiredField != null) {
                    return new MissingRequired(item, requiredField);
                }
                continue;
            }
            for (ConsentField field : item.dataFields) {
                if (field.required && !isFieldGranted(item.key(), field.key())) {
                    return new MissingRequired(item, field);
                }
            }
        }
        return null;
    }

    /**
     * Dung object {@code values} gui len /sendData: moi purpose la mot object long
     * gom {@code isAccept} va cac truong du lieu theo id.
     */
    public Map<String, Object> toValues() {
        Map<String, Object> values = new LinkedHashMap<>(extras);
        for (Map.Entry<String, Boolean> purpose : purposes.entrySet()) {
            boolean purposeAccepted = Boolean.TRUE.equals(purpose.getValue());
            Map<String, Object> node = new LinkedHashMap<>();
            node.put(KEY_IS_ACCEPT, purposeAccepted);

            Map<String, FieldDecision> group = fields.get(purpose.getKey());
            if (group != null) {
                for (Map.Entry<String, FieldDecision> field : group.entrySet()) {
                    Map<String, Object> fieldNode = new LinkedHashMap<>();
                    // Gia tri luon duoc gui du toggle bat hay tat — bang chung phai ghi ro nguoi dung
                    // da tu choi chinh xac du lieu nao.
                    if (field.getValue().value != null) {
                        fieldNode.put(KEY_VALUE, field.getValue().value);
                    }
                    // Muc dich tat thi truong con khong the la true — chot lai lan cuoi truoc khi gui.
                    fieldNode.put(KEY_IS_ACCEPT, purposeAccepted && field.getValue().accepted);
                    node.put(field.getKey(), fieldNode);
                }
            }
            values.put(purpose.getKey(), node);
        }
        return values;
    }

    private FieldDecision decision(String purposeKey, String fieldKey, boolean create) {
        Map<String, FieldDecision> group = fields.get(purposeKey);
        if (group == null) {
            if (!create) {
                return null;
            }
            group = new LinkedHashMap<>();
            fields.put(purposeKey, group);
        }
        FieldDecision decision = group.get(fieldKey);
        if (decision == null && create) {
            decision = new FieldDecision(false, null);
            group.put(fieldKey, decision);
        }
        return decision;
    }

    public String getConsentId() {
        return consentId;
    }

    void setConsentId(String consentId) {
        this.consentId = consentId;
    }

    public String getConfigCode() {
        return configCode;
    }

    void setConfigCode(String configCode) {
        this.configCode = configCode;
    }

    public long getSubmittedAtMs() {
        return submittedAtMs;
    }

    void setSubmittedAtMs(long submittedAtMs) {
        this.submittedAtMs = submittedAtMs;
    }
}
