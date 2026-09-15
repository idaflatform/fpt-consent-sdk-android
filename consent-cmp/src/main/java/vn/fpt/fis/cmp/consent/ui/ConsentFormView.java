package vn.fpt.fis.cmp.consent.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.List;

import vn.fpt.fis.cmp.consent.ConsentCallback;
import vn.fpt.fis.cmp.consent.ConsentCmp;
import vn.fpt.fis.cmp.consent.ConsentException;
import vn.fpt.fis.cmp.consent.ConsentState;
import vn.fpt.fis.cmp.consent.R;
import vn.fpt.fis.cmp.consent.model.ConsentConfig;
import vn.fpt.fis.cmp.consent.model.ConsentField;
import vn.fpt.fis.cmp.consent.model.ConsentItem;
import vn.fpt.fis.cmp.consent.model.SendConsentResult;

/**
 * Khoi UI "Danh sach su dong y": header dieu khien, danh sach purpose (thu gon / mo rong),
 * moi purpose co cac truong du lieu kem chip ben thu ba va toggle rieng.
 *
 * <p>Dung duoc doc lap trong layout cua app, hoac qua {@link ConsentSheetFragment}.</p>
 */
public class ConsentFormView extends LinearLayout {

    /** Su kien nguoi dung bam nut o cuoi form. */
    public interface OnActionListener {

        void onSubmit(ConsentState state);

        void onRejectAll(ConsentState state);
    }

    /** Ket qua khi view tu goi /sendData (che do dung doc lap, khong nam trong form cua app). */
    public interface OnSubmitResultListener {

        void onSubmitted(ConsentState state, SendConsentResult result);

        default void onSubmitFailed(ConsentException error) {
        }
    }

    /**
     * Bao moi lan nguoi dung doi lua chon — dung khi app tu ve nut submit va can bat/tat nut theo
     * dieu kien "da dong y het muc dich bat buoc".
     */
    public interface OnStateChangeListener {

        /**
         * @param state              lua chon hien tai
         * @param allRequiredGranted false khi con muc dich bat buoc chua duoc bat
         */
        void onStateChanged(ConsentState state, boolean allRequiredGranted);
    }

    private TextView tvSection;
    private TextView tvTitle;
    private TextView tvDescription;
    private TextView tvError;
    private TextView btnToggleList;
    private TextView btnCollapseAll;
    private TextView btnReject;
    private TextView btnSubmit;
    private SwitchMaterial swAcceptAll;
    private LinearLayout containerPurposes;
    private ProgressBar progress;

    private LayoutInflater themedInflater;
    private final List<PurposeHolder> holders = new ArrayList<>();
    private ConsentConfig config;
    private ConsentState state = new ConsentState();
    private OnActionListener actionListener;
    private OnStateChangeListener stateChangeListener;
    private OnSubmitResultListener submitResultListener;

    /** Chan vong lap khi code tu set trang thai switch. */
    private boolean binding;
    private boolean listHidden;
    private boolean allCollapsed;
    /** true = nam trong form cua app -> an nut cua SDK. */
    private boolean embeddedInForm;
    /** true = chi hien chip ben thu ba o truong co {@code sharedWithSystem = true}. */
    private boolean thirdPartiesOnSharedOnly;

    public ConsentFormView(Context context) {
        super(context);
        init(context, null);
    }

    public ConsentFormView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public ConsentFormView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, @Nullable AttributeSet attrs) {
        setOrientation(VERTICAL);
        // Inflate qua theme Bridge de SwitchMaterial chay duoc ca khi app dung theme AppCompat.
        themedInflater = LayoutInflater.from(new ContextThemeWrapper(context, R.style.Cmp_ViewTheme));
        themedInflater.inflate(R.layout.cmp_view_consent_form, this, true);

        tvSection = findViewById(R.id.cmp_tv_section);
        tvTitle = findViewById(R.id.cmp_tv_title);
        tvDescription = findViewById(R.id.cmp_tv_description);
        tvError = findViewById(R.id.cmp_tv_error);
        btnToggleList = findViewById(R.id.cmp_btn_toggle_list);
        btnCollapseAll = findViewById(R.id.cmp_btn_collapse_all);
        btnReject = findViewById(R.id.cmp_btn_reject);
        btnSubmit = findViewById(R.id.cmp_btn_submit);
        swAcceptAll = findViewById(R.id.cmp_sw_accept_all);
        containerPurposes = findViewById(R.id.cmp_container_purposes);
        progress = findViewById(R.id.cmp_progress);

        btnToggleList.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setListHidden(!listHidden);
            }
        });
        btnCollapseAll.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setAllCollapsed(!allCollapsed);
            }
        });
        swAcceptAll.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton button, boolean checked) {
                if (!binding) {
                    setAllGranted(checked);
                }
            }
        });
        btnReject.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setAllGranted(false);
                if (actionListener != null) {
                    actionListener.onRejectAll(state);
                } else {
                    // Tu choi tat ca cung la mot quyet dinh -> van ghi nhan bang chung.
                    sendConsent();
                }
            }
        });
        btnSubmit.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!validateRequired()) {
                    return;
                }
                if (actionListener != null) {
                    actionListener.onSubmit(state);
                } else {
                    sendConsent();
                }
            }
        });

        readAttrs(context, attrs);
    }

    private void readAttrs(Context context, @Nullable AttributeSet attrs) {
        boolean inForm = false;
        boolean showSection = true;
        if (attrs != null) {
            TypedArray values = context.obtainStyledAttributes(attrs, R.styleable.ConsentFormView);
            try {
                inForm = values.getBoolean(R.styleable.ConsentFormView_cmpEmbeddedInForm, false);
                showSection = values.getBoolean(R.styleable.ConsentFormView_cmpShowSectionTitle, true);
                thirdPartiesOnSharedOnly = values.getBoolean(
                        R.styleable.ConsentFormView_cmpThirdPartiesOnSharedOnly, false);
            } finally {
                values.recycle();
            }
        }
        setEmbeddedInForm(inForm);
        setSectionTitleVisible(showSection);
    }

    /**
     * Chi hien chip ben thu ba o truong co {@code sharedWithSystem = true}.
     *
     * <p>Mac dinh {@code false}: chip hien duoi moi truong cua purpose (giong ban web/portal), vi
     * {@code /config} tra {@code thirdParties} o muc purpose chu khong theo tung truong. Bat co nay
     * khi ban muon UI chi noi "chia se cho ai" o dung truong duoc chia se.</p>
     *
     * <p>Tuong duong thuoc tinh XML {@code app:cmpThirdPartiesOnSharedOnly}. Goi truoc {@link #bind}.</p>
     */
    public void setThirdPartiesOnSharedOnly(boolean onSharedOnly) {
        this.thirdPartiesOnSharedOnly = onSharedOnly;
    }

    /**
     * Khai bao view dang nam trong form cua app hay dung doc lap.
     *
     * <p>{@code true}: an hang nut cua SDK — app tu goi {@link ConsentCmp#submit} cung luc gui form
     * cua minh. {@code false} (mac dinh): hien nut "Tu choi tat ca" / "Dong y" va SDK tu goi
     * {@code /sendData} khi nguoi dung bam, tru khi app da dat {@link OnActionListener}.</p>
     *
     * <p>Tuong duong thuoc tinh XML {@code app:cmpEmbeddedInForm}.</p>
     */
    public void setEmbeddedInForm(boolean embeddedInForm) {
        this.embeddedInForm = embeddedInForm;
        setActionsVisible(!embeddedInForm);
    }

    /** True khi view dang o che do "nam trong form cua app" (nut cua SDK bi an). */
    public boolean isEmbeddedInForm() {
        return embeddedInForm;
    }

    /** Nhan ket qua khi SDK tu gui consent (che do dung doc lap). */
    public void setOnSubmitResultListener(@Nullable OnSubmitResultListener listener) {
        this.submitResultListener = listener;
    }

    /** Tu goi /sendData bang lua chon hien tai — dung o che do dung doc lap. */
    private void sendConsent() {
        if (!ConsentCmp.isInitialized()) {
            setError(getContext().getString(R.string.cmp_error_generic));
            return;
        }
        setSubmitting(true);
        ConsentCmp.get().submit(state, new ConsentCallback<SendConsentResult>() {
            @Override
            public void onSuccess(SendConsentResult result) {
                setSubmitting(false);
                setError(null);
                if (submitResultListener != null) {
                    submitResultListener.onSubmitted(state, result);
                }
            }

            @Override
            public void onError(ConsentException error) {
                setSubmitting(false);
                setError(error.getMessage());
                if (submitResultListener != null) {
                    submitResultListener.onSubmitFailed(error);
                }
            }
        });
    }

    public void setOnActionListener(@Nullable OnActionListener listener) {
        this.actionListener = listener;
    }

    public void setOnStateChangeListener(@Nullable OnStateChangeListener listener) {
        this.stateChangeListener = listener;
    }

    /** Muc dich bat buoc dau tien chua duoc dong y; null khi khong thieu purpose nao. */
    @Nullable
    public ConsentItem missingRequired() {
        return state.firstMissingRequired(config);
    }

    /** Muc bat buoc dau tien con thieu — purpose hoac truong du lieu; null khi hop le. */
    @Nullable
    public ConsentState.MissingRequired missing() {
        return state.firstMissing(config);
    }

    /** True khi moi muc dich VA truong du lieu bat buoc da duoc dong y. */
    public boolean hasAllRequired() {
        return missing() == null;
    }

    /** Thong bao cho muc bat buoc con thieu (purpose hoac truong); null khi hop le. */
    @Nullable
    public String requiredMessage() {
        ConsentState.MissingRequired missing = missing();
        return missing == null ? null : requiredMessageFor(missing.item, missing.field);
    }

    /** Thong bao "khong the tu choi" cho mot purpose (field = null) hoac mot truong du lieu. */
    private String requiredMessageFor(ConsentItem item, @Nullable ConsentField field) {
        if (field == null) {
            return getContext().getString(R.string.cmp_error_required, item.label);
        }
        return getContext().getString(R.string.cmp_error_field_required,
                item.label, field.displayName());
    }

    /**
     * Kiem tra muc dich / truong du lieu bat buoc va hien loi ngay tren form neu thieu.
     *
     * @return true khi hop le
     */
    public boolean validateRequired() {
        String message = requiredMessage();
        setError(message);
        return message == null;
    }

    /** An dong tieu de nho phia tren the (mac dinh hien). */
    public void setSectionTitleVisible(boolean visible) {
        tvSection.setVisibility(visible ? VISIBLE : GONE);
    }

    /** An hang nut Tu choi / Dong y khi app tu ve nut rieng. */
    public void setActionsVisible(boolean visible) {
        findViewById(R.id.cmp_container_actions).setVisibility(visible ? VISIBLE : GONE);
    }

    /**
     * Ve toan bo form tu cau hinh /config.
     *
     * <p>{@code initial} duoc gop theo cau hinh dang hien thi: purpose / truong khong con trong
     * config se bi bo, purpose / truong moi lay gia tri mac dinh. Nho vay du lieu cu cua mot ban
     * cau hinh da doi khong lot vao request moi.</p>
     *
     * @param initial quyet dinh khoi tao (vd lan truoc da luu); null = lay mac dinh cua config
     */
    public void bind(ConsentConfig config, @Nullable ConsentState initial) {
        this.config = config;
        this.state = ConsentState.merge(config, initial);
        holders.clear();
        containerPurposes.removeAllViews();

        if (config == null) {
            return;
        }
        if (config.config != null && config.config.title != null) {
            tvTitle.setText(config.config.title);
        }
        if (config.config != null && config.config.description != null) {
            tvDescription.setText(config.config.description);
            tvDescription.setVisibility(VISIBLE);
        } else {
            tvDescription.setVisibility(GONE);
        }
        if (config.config != null && config.config.submitLabel != null) {
            btnSubmit.setText(config.config.submitLabel);
        }

        LayoutInflater inflater = themedInflater;
        for (ConsentItem item : config.items()) {
            PurposeHolder holder = new PurposeHolder(inflater, containerPurposes, item);
            holders.add(holder);
            containerPurposes.addView(holder.root);
        }
        refreshFromState();
    }

    /** Quyet dinh hien tai tren UI. */
    public ConsentState state() {
        return state;
    }

    public void setError(@Nullable CharSequence message) {
        if (message == null || message.length() == 0) {
            tvError.setVisibility(GONE);
        } else {
            tvError.setText(message);
            tvError.setVisibility(VISIBLE);
        }
    }

    /** Khoa nut + hien vong quay trong luc goi /sendData. */
    public void setSubmitting(boolean submitting) {
        progress.setVisibility(submitting ? VISIBLE : GONE);
        btnSubmit.setEnabled(!submitting);
        btnReject.setEnabled(!submitting);
    }

    private void setListHidden(boolean hidden) {
        this.listHidden = hidden;
        containerPurposes.setVisibility(hidden ? GONE : VISIBLE);
        btnToggleList.setText(hidden ? R.string.cmp_show_list : R.string.cmp_hide_list);
    }

    private void setAllCollapsed(boolean collapsed) {
        this.allCollapsed = collapsed;
        for (PurposeHolder holder : holders) {
            holder.setExpanded(!collapsed);
        }
        btnCollapseAll.setText(collapsed ? R.string.cmp_expand : R.string.cmp_collapse);
    }


    /**
     * Nguoi dung bam "Dong y tat ca" / "Tu choi tat ca".
     *
     * <p>Quy tac nam trong {@link ConsentState} de moi platform dung chung; view chi ve lai va hien
     * thong bao khi thao tac bi chan boi muc/truong bat buoc.</p>
     */
    private void setAllGranted(boolean granted) {
        if (granted) {
            state.applyAcceptAll(config);
            setError(null);
        } else {
            ConsentState.ToggleResult result = state.applyRejectAll(config);
            setError(result == ConsentState.ToggleResult.BLOCKED_REQUIRED ? lockedNotice() : null);
        }
        refreshFromState();
    }

    /** Thong bao ve muc bat buoc khong the tu choi; null khi cau hinh khong co muc nao bat buoc. */
    @Nullable
    private String lockedNotice() {
        if (config == null) {
            return null;
        }
        for (ConsentItem item : config.items()) {
            if (item.required) {
                return requiredMessageFor(item, null);
            }
            ConsentField requiredField = item.firstRequiredField();
            if (requiredField != null) {
                return requiredMessageFor(item, requiredField);
            }
        }
        return null;
    }

    /**
     * Ve lai toan bo toggle theo {@link #state}, roi bao trang thai ra ngoai.
     *
     * <p>Moi thay doi lua chon deu di qua day: state la nguon su that duy nhat, view khong tu suy
     * dien quy tac nao. Duoc goi ca o lan {@link #bind} dau tien.</p>
     */
    private void refreshFromState() {
        binding = true;
        for (PurposeHolder holder : holders) {
            holder.syncFromState();
        }
        swAcceptAll.setChecked(state.isAllGranted());
        binding = false;
        if (stateChangeListener != null) {
            stateChangeListener.onStateChanged(state, hasAllRequired());
        }
    }

    // ==================== Holders ====================

    /** Mot the purpose + cac truong du lieu ben trong. Chi lo phan hien thi. */
    private final class PurposeHolder {

        final ConsentItem item;
        final View root;
        final SwitchMaterial toggle;
        final TextView btnExpand;
        final LinearLayout fieldsContainer;
        final View divider;
        final List<FieldHolder> fields = new ArrayList<>();
        boolean expanded = true;

        PurposeHolder(LayoutInflater inflater, ViewGroup parent, final ConsentItem item) {
            this.item = item;
            this.root = inflater.inflate(R.layout.cmp_item_purpose, parent, false);
            this.toggle = root.findViewById(R.id.cmp_sw_purpose);
            this.btnExpand = root.findViewById(R.id.cmp_btn_purpose_toggle);
            this.fieldsContainer = root.findViewById(R.id.cmp_container_fields);
            this.divider = root.findViewById(R.id.cmp_divider);

            TextView label = root.findViewById(R.id.cmp_tv_purpose_label);
            label.setText(item.label);

            // Nhan "Bat buoc" cho purpose khong the tu choi.
            TextView badge = root.findViewById(R.id.cmp_tv_purpose_badge);
            badge.setVisibility(item.required ? VISIBLE : GONE);

            TextView description = root.findViewById(R.id.cmp_tv_purpose_description);
            if (item.description != null && item.description.length() > 0) {
                description.setText(item.description);
                description.setVisibility(VISIBLE);
            } else {
                description.setVisibility(GONE);
            }

            for (ConsentField field : item.dataFields) {
                // /config tra ve ca truong khong duoc chon cho purpose — chi de app biet form nguon
                // thu nhung gi. Khong dung UI cho chung (R16).
                if (!field.display) {
                    continue;
                }
                FieldHolder holder = new FieldHolder(inflater, fieldsContainer, item, field);
                fields.add(holder);
                fieldsContainer.addView(holder.root);
            }
            boolean hasFields = !fields.isEmpty();
            divider.setVisibility(hasFields ? VISIBLE : GONE);
            fieldsContainer.setVisibility(hasFields ? VISIBLE : GONE);
            btnExpand.setVisibility(hasFields ? VISIBLE : GONE);

            binding = true;
            toggle.setChecked(state.isGranted(item.key()));
            binding = false;

            // Toggle van bam duoc ke ca voi muc bat buoc: co bam moi hien duoc ly do
            // (view bi setEnabled(false) khong nhan touch nen khong noi duoc gi cho nguoi dung).
            toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton button, boolean checked) {
                    if (binding) {
                        return;
                    }
                    ConsentState.ToggleResult result =
                            state.applyPurposeToggle(config, item.key(), checked);
                    setError(result == ConsentState.ToggleResult.BLOCKED_REQUIRED
                            ? requiredMessageFor(item, item.required ? null : item.firstRequiredField())
                            : null);
                    refreshFromState();
                }
            });
            btnExpand.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    setExpanded(!expanded);
                }
            });
        }

        void setExpanded(boolean expanded) {
            this.expanded = expanded;
            boolean hasFields = !fields.isEmpty();
            fieldsContainer.setVisibility(expanded && hasFields ? VISIBLE : GONE);
            divider.setVisibility(expanded && hasFields ? VISIBLE : GONE);
            btnExpand.setText(expanded ? R.string.cmp_collapse : R.string.cmp_expand);
        }

        /** Ve lai theo state; caller phai dat {@code binding = true} truoc khi goi. */
        void syncFromState() {
            toggle.setChecked(state.isGranted(item.key()));
            for (FieldHolder field : fields) {
                field.syncFromState();
            }
        }
    }

    /** Mot dong truong du lieu trong the purpose. Chi lo phan hien thi. */
    private final class FieldHolder {

        final View root;
        final SwitchMaterial toggle;
        final String purposeKey;
        final String fieldKey;

        FieldHolder(LayoutInflater inflater, ViewGroup parent, final ConsentItem item,
                final ConsentField field) {
            this.root = inflater.inflate(R.layout.cmp_item_field, parent, false);
            this.toggle = root.findViewById(R.id.cmp_sw_field);
            this.purposeKey = item.key();
            this.fieldKey = field.key();

            TextView title = root.findViewById(R.id.cmp_tv_field_title);
            title.setText(field.displayName());

            // Nhan "Bat buoc" cho truong du lieu khong the tu choi rieng.
            root.findViewById(R.id.cmp_tv_field_badge).setVisibility(field.required ? VISIBLE : GONE);

            // Chip ben thu ba, hien ngay duoi ten truong. Mac dinh hien cho moi truong cua purpose;
            // bat thirdPartiesOnSharedOnly de chi hien o truong co sharedWithSystem = true.
            LinearLayout parties = root.findViewById(R.id.cmp_container_parties);
            View scroll = root.findViewById(R.id.cmp_scroll_parties);
            boolean showParties = !item.thirdParties.isEmpty()
                    && (!thirdPartiesOnSharedOnly || field.sharedWithSystem);
            if (showParties) {
                for (String name : item.thirdParties) {
                    TextView chip = (TextView) inflater.inflate(R.layout.cmp_item_chip, parties, false);
                    chip.setText(name);
                    parties.addView(chip);
                }
                scroll.setVisibility(VISIBLE);
            } else {
                scroll.setVisibility(GONE);
            }

            binding = true;
            toggle.setChecked(state.isFieldGranted(purposeKey, fieldKey));
            binding = false;

            toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton button, boolean checked) {
                    if (binding) {
                        return;
                    }
                    ConsentState.ToggleResult result =
                            state.applyFieldToggle(config, purposeKey, fieldKey, checked);
                    setError(result == ConsentState.ToggleResult.BLOCKED_REQUIRED
                            ? requiredMessageFor(item, field) : null);
                    refreshFromState();
                }
            });
        }

        /** Ve lai theo state; caller phai dat {@code binding = true} truoc khi goi. */
        void syncFromState() {
            toggle.setChecked(state.isFieldGranted(purposeKey, fieldKey));
        }
    }
}
