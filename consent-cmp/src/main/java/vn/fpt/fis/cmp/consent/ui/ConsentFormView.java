package vn.fpt.fis.cmp.consent.ui;

import vn.fpt.fis.cmp.consent.ConsentAppearance;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import vn.fpt.fis.cmp.consent.ConsentCallback;
import vn.fpt.fis.cmp.consent.ConsentCmp;
import vn.fpt.fis.cmp.consent.ConsentException;
import vn.fpt.fis.cmp.consent.ConsentState;
import vn.fpt.fis.cmp.consent.ConsentValueSource;
import vn.fpt.fis.cmp.consent.R;
import vn.fpt.fis.cmp.consent.model.ConsentConfig;
import vn.fpt.fis.cmp.consent.model.ConsentField;
import vn.fpt.fis.cmp.consent.model.ConsentItem;
import vn.fpt.fis.cmp.consent.model.SendConsentResult;

/**
 * Khoi consent dang accordion: tieu de + "Dong y tat ca", danh sach muc dich (moi luc mo 1 muc),
 * moi muc co mo ta, ben nhan du lieu ("Chia se voi") va cac truong du lieu chon / bo chon.
 *
 * <p>Tu doi bo cuc theo do rong cua chinh view: duoi 560dp truong du lieu la chip, "Dong y tat ca"
 * la 1 hang rieng; tu 560dp tro len la checkbox 2 cot, "Dong y tat ca" canh tieu de.</p>
 *
 * <p>Dung duoc doc lap trong layout cua app, hoac qua {@link ConsentSheetFragment}. Moi quy tac
 * bat / tat nam trong {@link ConsentState}; view chi ve lai theo state.</p>
 */
public class ConsentFormView extends LinearLayout {

    /** Su kien nguoi dung bam nut o cuoi form. */
    public interface OnActionListener {

        void onSubmit(ConsentState state);

        /**
         * @deprecated Giao dien hien tai khong con nut "Tu choi tat ca" (tat "Dong y tat ca" roi
         * luu la tu choi) — ham nay khong con duoc goi.
         */
        @Deprecated
        default void onRejectAll(ConsentState state) {
        }
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

    /** Kết quả khi view tự tải {@code /config} (autoload hoặc {@link #reload()}). */
    public interface OnLoadListener {

        /** Đã tải và vẽ xong form. */
        void onLoaded(ConsentConfig config);

        /** Tải lỗi; view đã hiện nút "Thử lại". */
        default void onLoadFailed(ConsentException error) {
        }
    }

    /** Mo ta dai hon nguong nay thi cat 3 dong + nut "Xem them" (giong ban React). */
    private static final int DESCRIPTION_CLAMP_CHARS = 120;
    private static final int DESCRIPTION_CLAMP_LINES = 3;

    private TextView tvSection;
    private TextView tvTitle;
    private TextView tvDescription;
    private TextView tvError;
    private TextView tvSummary;
    private TextView tvToggleList;
    private ImageView ivToggleList;
    private View btnToggleList;
    private TextView btnSubmit;
    private TextView tvAcceptAll;
    private SwitchMaterial swAcceptAll;
    private LinearLayout contentView;
    private LinearLayout headerRow;
    private LinearLayout masterSlot;
    private LinearLayout masterView;
    private LinearLayout containerPurposes;
    private View footer;
    private View footerDivider;
    private View actionsView;
    private ProgressBar progress;
    private View loadingView;
    private View errorStateView;
    private TextView tvErrorState;

    private LayoutInflater themedInflater;
    private final List<PurposeHolder> holders = new ArrayList<>();
    private ConsentConfig config;
    private ConsentState state = new ConsentState();
    private OnActionListener actionListener;
    private OnStateChangeListener stateChangeListener;
    private OnSubmitResultListener submitResultListener;
    private OnLoadListener loadListener;

    /** Chan vong lap khi code tu set trang thai switch. */
    private boolean binding;
    /** Đang gọi /sendData — chặn bấm lặp và khoá mọi toggle. */
    private boolean submitting;
    /** true = tự gọi /config khi gắn vào màn hình. */
    private boolean autoLoad;
    private boolean loading;
    /** Bỏ qua callback của lần tải cũ. */
    private int loadGeneration;
    /** true = nam trong form cua app -> an nut cua SDK, danh sach thu gon sau "Xem N muc dich". */
    private boolean embeddedInForm;
    private boolean actionsVisible = true;
    /** Danh sach muc dich dang thu gon (chi dung khi {@link #embeddedInForm}). */
    private boolean listCollapsed;
    /** true = chi lay ben nhan du lieu tu truong co {@code sharedWithSystem = true}. */
    private boolean thirdPartiesOnSharedOnly;
    /** Bo cuc man rong (>= cmp_wide_min_width). */
    private boolean wide;
    private boolean compact;
    /** Accordion: key muc dang mo; null = khong mo muc nao. */
    @Nullable
    private String openKey;

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

    /**
     * Bo nen ripple cua switch tren API &lt; 26: Android 7.x abort RenderThread khi ripple chay trong
     * PlatformView cua Flutter / React Native. Switch van doi trang thai binh thuong.
     */
    private static void stripRippleOnOldApi(View view) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            view.setBackground(null);
        }
    }

    /** Switch theo thiet ke: ranh day 48x26dp, thumb trang nam trong, xanh khi bat / xam khi tat. */
    private void styleSwitch(SwitchMaterial toggle) {
        stripRippleOnOldApi(toggle);
        toggle.setUseMaterialThemeColors(false);
        toggle.setThumbResource(R.drawable.cmp_switch_thumb);
        toggle.setTrackResource(R.drawable.cmp_switch_track);
        toggle.setSwitchMinWidth(dp(48));
        toggle.setShowText(false);
        toggle.setThumbTintList(ColorStateList.valueOf(color(R.color.cmp_switch_thumb_off)));
        toggle.setTrackTintList(new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{color(R.color.cmp_primary), color(R.color.cmp_switch_track_off)}));
    }

    private ConsentAppearance appearance = ConsentAppearance.defaults();
    private boolean appearanceOverride;
    public void setAppearance(ConsentAppearance value) {
        appearanceOverride = true; appearance = value == null ? ConsentAppearance.defaults() : value;
        applyLayoutMode(); applyAppearance();
    }
    private void applyAppearance() {
        AppearanceRenderer.apply(this, appearance, ConsentCmp.isInitialized() ? ConsentCmp.get().appearanceRepository() : null);
    }
    private void appearanceBackground(View view, int res) {
        view.setBackgroundResource(res);
        view.setTag(R.id.cmp_appearance_background, res == 0 ? "" : AppearanceRenderer.backgroundName(res));
    }

    private int color(int res) {
        return appearance.color(AppearanceRenderer.token(res), ContextCompat.getColor(getContext(), res));
    }

    private int dp(float value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics()));
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
        tvSummary = findViewById(R.id.cmp_tv_summary);
        btnToggleList = findViewById(R.id.cmp_btn_toggle_list);
        tvToggleList = findViewById(R.id.cmp_tv_toggle_list);
        ivToggleList = findViewById(R.id.cmp_iv_toggle_list);
        btnSubmit = findViewById(R.id.cmp_btn_submit);
        tvAcceptAll = findViewById(R.id.cmp_tv_accept_all);
        swAcceptAll = findViewById(R.id.cmp_sw_accept_all);
        styleSwitch(swAcceptAll);
        contentView = findViewById(R.id.cmp_form_content);
        // Nen cua hang dang mo / nut "Xem N muc dich" khong de len vien bo goc cua the:
        // thut 1dp (do day vien) va cat con theo goc bo.
        contentView.setPadding(dp(1), dp(1), dp(1), dp(1));
        contentView.setClipToOutline(true);
        headerRow = findViewById(R.id.cmp_header_row);
        masterSlot = findViewById(R.id.cmp_master_slot);
        masterView = findViewById(R.id.cmp_master);
        containerPurposes = findViewById(R.id.cmp_container_purposes);
        footer = findViewById(R.id.cmp_footer);
        footerDivider = findViewById(R.id.cmp_footer_divider);
        actionsView = findViewById(R.id.cmp_container_actions);
        progress = findViewById(R.id.cmp_progress);
        loadingView = findViewById(R.id.cmp_form_state_loading);
        errorStateView = findViewById(R.id.cmp_form_state_error);
        tvErrorState = findViewById(R.id.cmp_form_tv_state_error);

        findViewById(R.id.cmp_form_btn_retry).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                reload();
            }
        });
        btnToggleList.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setListCollapsed(!listCollapsed);
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
        btnSubmit.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (actionListener != null) {
                    if (validateRequired()) {
                        actionListener.onSubmit(state);
                    }
                } else {
                    submit(resultNotifier());
                }
            }
        });

        readAttrs(context, attrs);
    }

    private void readAttrs(Context context, @Nullable AttributeSet attrs) {
        boolean inForm = false;
        boolean showSection = false;
        if (attrs != null) {
            TypedArray values = context.obtainStyledAttributes(attrs, R.styleable.ConsentFormView);
            try {
                inForm = values.getBoolean(R.styleable.ConsentFormView_cmpEmbeddedInForm, false);
                showSection = values.getBoolean(R.styleable.ConsentFormView_cmpShowSectionTitle, false);
                thirdPartiesOnSharedOnly = values.getBoolean(
                        R.styleable.ConsentFormView_cmpThirdPartiesOnSharedOnly, false);
                autoLoad = values.getBoolean(R.styleable.ConsentFormView_cmpAutoLoad, false);
            } finally {
                values.recycle();
            }
        }
        setEmbeddedInForm(inForm);
        setSectionTitleVisible(showSection);
    }

    /**
     * Chi lay ben nhan du lieu ("Chia se voi") khi muc co truong {@code sharedWithSystem = true}.
     *
     * <p>Mac dinh {@code false}: hien {@code thirdParties} cua muc dich (giong ban web/portal).</p>
     *
     * <p>Tuong duong thuoc tinh XML {@code app:cmpThirdPartiesOnSharedOnly}. Goi truoc {@link #bind}.</p>
     */
    public void setThirdPartiesOnSharedOnly(boolean onSharedOnly) {
        this.thirdPartiesOnSharedOnly = onSharedOnly;
    }

    /**
     * Khai bao view dang nam trong form cua app hay dung doc lap.
     *
     * <p>{@code true}: an chan khoi cua SDK — app tu goi {@link #submit} cung luc gui form cua minh;
     * danh sach muc dich thu gon sau nut "Xem N muc dich". {@code false} (mac dinh): hien
     * "x / N muc dich da chon" + nut gui; SDK tu goi {@code /sendData} khi nguoi dung bam, tru khi
     * app da dat {@link OnActionListener}.</p>
     *
     * <p>Tuong duong thuoc tinh XML {@code app:cmpEmbeddedInForm}.</p>
     */
    public void setEmbeddedInForm(boolean embeddedInForm) {
        this.embeddedInForm = embeddedInForm;
        setActionsVisible(!embeddedInForm);
        setListCollapsed(embeddedInForm);
    }

    /** True khi view dang o che do "nam trong form cua app" (nut cua SDK bi an). */
    public boolean isEmbeddedInForm() {
        return embeddedInForm;
    }

    /** Nhan ket qua khi SDK tu gui consent (che do dung doc lap). */
    public void setOnSubmitResultListener(@Nullable OnSubmitResultListener listener) {
        this.submitResultListener = listener;
    }

    /** Chuyển kết quả sang {@link OnSubmitResultListener}. */
    private ConsentCallback<SendConsentResult> resultNotifier() {
        return new ConsentCallback<SendConsentResult>() {
            @Override
            public void onSuccess(SendConsentResult result) {
                if (submitResultListener != null) {
                    submitResultListener.onSubmitted(state, result);
                }
            }

            @Override
            public void onError(ConsentException error) {
                if (submitResultListener != null) {
                    submitResultListener.onSubmitFailed(error);
                }
            }
        };
    }

    // ==================== Gửi dữ liệu ====================

    /**
     * Kiểm tra mục bắt buộc, gửi {@code /sendData}, hiện lỗi trên form. Bấm lặp khi đang gửi bị bỏ qua.
     * Giá trị trường lấy từ {@code ConsentCmp.setValueSource}/{@code bindForm}, không có thì quét
     * ô nhập có {@code android:tag} trên màn hình.
     */
    @MainThread
    public void submit(@Nullable ConsentCallback<SendConsentResult> callback) {
        submit(null, callback);
    }

    /** Như {@link #submit(ConsentCallback)}, truyền thẳng giá trị trường cho lần gửi này. */
    @MainThread
    public void submit(@Nullable ConsentValueSource values,
                       @Nullable final ConsentCallback<SendConsentResult> callback) {
        if (submitting) {
            // Chặn bấm lặp.
            return;
        }
        if (!ConsentCmp.isInitialized()) {
            fail(callback, new ConsentException(getContext().getString(R.string.cmp_error_not_initialized)));
            return;
        }
        if (config == null) {
            fail(callback, new ConsentException(getContext().getString(R.string.cmp_error_no_config)));
            return;
        }
        String required = requiredMessage();
        if (required != null) {
            fail(callback, new ConsentException(required));
            return;
        }
        ConsentValueSource source = values != null ? values : ConsentCmp.get().valueSource();
        if (source == null) {
            source = new ViewFormValueSource(getRootView());
        }
        setError(null);
        setSubmitting(true);
        // Kiểm tra theo cấu hình form đang hiển thị.
        ConsentCmp.get().submit(state, config, source, new ConsentCallback<SendConsentResult>() {
            @Override
            public void onSuccess(SendConsentResult result) {
                setSubmitting(false);
                setError(null);
                if (callback != null) {
                    callback.onSuccess(result);
                }
            }

            @Override
            public void onError(ConsentException error) {
                setSubmitting(false);
                showError(error.getMessage() != null
                        ? error.getMessage() : getContext().getString(R.string.cmp_error_generic));
                if (callback != null) {
                    callback.onError(error);
                }
            }
        });
    }

    private void fail(@Nullable ConsentCallback<SendConsentResult> callback, ConsentException error) {
        // Danh sach dang thu gon thi mo ra de nguoi dung thay muc bat buoc.
        if (listCollapsed && config != null) {
            setListCollapsed(false);
        }
        showError(error.getMessage());
        if (callback != null) {
            callback.onError(error);
        }
    }

    /** Hiện lỗi và cuộn tới dòng lỗi. */
    private void showError(@Nullable CharSequence message) {
        setError(message);
        if (tvError.getVisibility() != VISIBLE) {
            return;
        }
        tvError.post(new Runnable() {
            @Override
            public void run() {
                tvError.requestRectangleOnScreen(
                        new Rect(0, 0, tvError.getWidth(), tvError.getHeight()), false);
            }
        });
    }

    // ==================== Tải cấu hình ====================

    /** Nhận kết quả khi view tự tải {@code /config}. */
    public void setOnLoadListener(@Nullable OnLoadListener listener) {
        this.loadListener = listener;
    }

    /** Tương đương {@code app:cmpAutoLoad}; bật khi view đã hiển thị thì tải ngay. */
    public void setAutoLoad(boolean autoLoad) {
        this.autoLoad = autoLoad;
        if (autoLoad && isAttachedToWindow()) {
            autoLoadIfNeeded();
        }
    }

    /** True khi đang tải {@code /config}. */
    public boolean isLoading() {
        return loading;
    }

    /** Tải lại {@code /config} rồi vẽ lại form; lỗi thì hiện nút "Thử lại". */
    @MainThread
    public void reload() {
        if (!ConsentCmp.isInitialized()) {
            showLoadError(new ConsentException(getContext().getString(R.string.cmp_error_not_initialized)));
            return;
        }
        final int generation = ++loadGeneration;
        loading = true;
        showState(loadingView);
        applyAppearance();
        ConsentCmp.get().fetchConfig(new ConsentCallback<ConsentConfig>() {
            @Override
            public void onSuccess(ConsentConfig result) {
                if (generation != loadGeneration) {
                    // Kết quả cũ, bỏ qua.
                    return;
                }
                bind(result, ConsentCmp.get().savedState());
                if (loadListener != null) {
                    loadListener.onLoaded(result);
                }
            }

            @Override
            public void onError(ConsentException error) {
                if (generation != loadGeneration) {
                    return;
                }
                loading = false;
                showLoadError(error);
            }
        });
    }

    private void showLoadError(ConsentException error) {
        if (!appearanceOverride && ConsentCmp.isInitialized()) appearance = ConsentCmp.get().appearance();
        tvErrorState.setText(error.getMessage() != null
                ? error.getMessage() : getContext().getString(R.string.cmp_error_generic));
        showState(errorStateView);
        applyAppearance();
        if (loadListener != null) {
            loadListener.onLoadFailed(error);
        }
    }

    private void showState(View visible) {
        contentView.setVisibility(visible == contentView ? VISIBLE : GONE);
        loadingView.setVisibility(visible == loadingView ? VISIBLE : GONE);
        errorStateView.setVisibility(visible == errorStateView ? VISIBLE : GONE);
    }

    private void autoLoadIfNeeded() {
        if (autoLoad && config == null && !loading && !isInEditMode()) {
            reload();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        autoLoadIfNeeded();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (loading) {
            // Bỏ kết quả đang tải; gắn lại thì tải lại.
            loadGeneration++;
            loading = false;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED) {
            int available = Math.max(0, MeasureSpec.getSize(widthMeasureSpec)
                    - getPaddingLeft() - getPaddingRight());
            boolean shouldBeWide = available >= getResources().getDimensionPixelSize(
                    R.dimen.cmp_wide_min_width);
            boolean shouldBeCompact = available < getResources().getDimensionPixelSize(
                    R.dimen.cmp_compact_min_width);
            if (shouldBeWide != wide || shouldBeCompact != compact) {
                wide = shouldBeWide;
                compact = shouldBeCompact;
                // Apply before measuring so embedded hosts receive the height of the final layout.
                applyLayoutMode();
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
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

    /** Dong tieu de nho phia tren the (mac dinh an — thiet ke accordion khong dung). */
    public void setSectionTitleVisible(boolean visible) {
        tvSection.setVisibility(visible ? VISIBLE : GONE);
    }

    /** An / hien chan khoi ("x / N muc dich da chon" + nut gui) khi app tu ve nut rieng. */
    public void setActionsVisible(boolean visible) {
        actionsVisible = visible;
        actionsView.setVisibility(visible ? VISIBLE : GONE);
        updateFooterVisibility();
        applyAppearance();
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
        if (!appearanceOverride && ConsentCmp.isInitialized()) appearance = ConsentCmp.get().appearance();
        // Huỷ kết quả của lần tải đang dở.
        loadGeneration++;
        loading = false;
        showState(contentView);
        this.config = config;
        this.state = ConsentState.merge(config, initial);
        holders.clear();
        containerPurposes.removeAllViews();
        openKey = null;

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

        for (ConsentItem item : config.items()) {
            PurposeHolder holder = new PurposeHolder(themedInflater, containerPurposes, item);
            holders.add(holder);
            containerPurposes.addView(holder.root);
            // Mo san muc dau tien co noi dung de nguoi dung thay ngay cach chon truong.
            if (openKey == null && holder.hasBody) {
                openKey = item.key();
            }
        }
        tvToggleList.setText(getContext().getString(
                R.string.cmp_view_purposes, config.items().size()));
        setListCollapsed(listCollapsed);
        applyLayoutMode();
        setInputsEnabled(!submitting);
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
        updateFooterVisibility();
        applyAppearance();
    }

    /** Khoá form (cả các toggle) + hiện vòng quay trong lúc gọi /sendData. */
    public void setSubmitting(boolean submitting) {
        this.submitting = submitting;
        progress.setVisibility(submitting ? VISIBLE : GONE);
        btnSubmit.setEnabled(!submitting);
        setInputsEnabled(!submitting);
        applyAppearance();
    }

    /** True khi đang gọi /sendData. */
    public boolean isSubmitting() {
        return submitting;
    }

    private void setInputsEnabled(boolean enabled) {
        swAcceptAll.setEnabled(enabled);
        for (PurposeHolder holder : holders) {
            holder.setEnabled(enabled);
        }
    }

    /** Chan khoi chi chiem cho (va ke duong phan cach) khi co loi hoac co nut. */
    private void updateFooterVisibility() {
        boolean hasContent = tvError.getVisibility() == VISIBLE || actionsVisible;
        footer.setVisibility(hasContent ? VISIBLE : GONE);
        footerDivider.setVisibility(hasContent && wide ? VISIBLE : GONE);
    }

    private void setListCollapsed(boolean collapsed) {
        this.listCollapsed = collapsed && embeddedInForm;
        containerPurposes.setVisibility(listCollapsed || holders.isEmpty() ? GONE : VISIBLE);
        btnToggleList.setVisibility(embeddedInForm && !holders.isEmpty() ? VISIBLE : GONE);
        if (listCollapsed) {
            tvToggleList.setText(getContext().getString(R.string.cmp_view_purposes, holders.size()));
        } else {
            tvToggleList.setText(R.string.cmp_collapse);
        }
        ivToggleList.setRotation(listCollapsed ? 90 : -90);
    }

    /** Accordion: mo muc {@code key}, dong cac muc khac; {@code null} = dong het. */
    private void setOpen(@Nullable String key) {
        openKey = key;
        for (PurposeHolder holder : holders) { holder.render(); }
        applyAppearance();
    }

    private void textSize(TextView view, int regular, int small) {
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimension(compact ? small : regular));
    }

    /** Dat bo cuc hep / rong cho phan khung; tung muc tu ve lai theo {@link #wide}. */
    private void applyLayoutMode() {
        ViewGroup currentParent = (ViewGroup) masterView.getParent();
        ViewGroup targetParent = wide ? headerRow : masterSlot;
        if (currentParent != targetParent) {
            currentParent.removeView(masterView);
            targetParent.addView(masterView);
        }
        LinearLayout.LayoutParams labelLp = (LinearLayout.LayoutParams) tvAcceptAll.getLayoutParams();
        LinearLayout.LayoutParams masterLp = (LinearLayout.LayoutParams) masterView.getLayoutParams();
        if (wide) {
            masterLp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            masterLp.setMarginStart(dp(16));
            labelLp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            labelLp.weight = 0;
            masterView.setMinimumHeight(0);
            masterView.setPadding(0, 0, 0, 0);
            tvAcceptAll.setTextColor(color(R.color.cmp_text_secondary));
            tvAcceptAll.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimension(R.dimen.cmp_text_small));
        } else {
            masterLp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            masterLp.setMarginStart(0);
            labelLp.width = 0;
            labelLp.weight = 1;
            masterView.setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.cmp_touch));
            masterView.setPadding(dp(16), 0, dp(16), 0);
            tvAcceptAll.setTextColor(color(R.color.cmp_text_primary));
            textSize(tvAcceptAll, R.dimen.cmp_text_purpose, R.dimen.cmp_text_purpose_compact);
        }
        masterView.setLayoutParams(masterLp);
        tvAcceptAll.setLayoutParams(labelLp);
        masterSlot.setVisibility(wide ? GONE : VISIBLE);

        int side = dp(wide ? 20 : compact ? 12 : 16);
        headerRow.setPadding(side, wide ? dp(18) : dp(14), side, wide ? dp(14) : dp(12));
        if (wide) {
            tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        } else {
            textSize(tvTitle, R.dimen.cmp_text_title, R.dimen.cmp_text_title_compact);
            masterView.setPadding(side, 0, side, 0);
        }
        textSize(tvDescription, R.dimen.cmp_text_small, R.dimen.cmp_text_small_compact);
        for (TextView text : new TextView[]{tvSection, tvError, tvSummary, tvToggleList,
                btnSubmit, tvErrorState}) {
            textSize(text, R.dimen.cmp_text_body, R.dimen.cmp_text_body_compact);
        }

        // Man rong (thiet ke tablet): the nen xam, danh sach la khung trang rieng, chan khoi nen
        // trang; card khong ke duong giua cac khoi.
        appearanceBackground(contentView, wide ? R.drawable.cmp_bg_card_subtle : R.drawable.cmp_bg_card);
        contentView.setPadding(dp(1), dp(1), dp(1), dp(1));
        footer.setBackgroundColor(wide ? color(R.color.cmp_bg) : 0);
        contentView.setShowDividers(wide ? SHOW_DIVIDER_NONE : SHOW_DIVIDER_MIDDLE);
        LinearLayout.LayoutParams listLp =
                (LinearLayout.LayoutParams) containerPurposes.getLayoutParams();
        listLp.setMarginStart(wide ? dp(20) : 0);
        listLp.setMarginEnd(wide ? dp(20) : 0);
        containerPurposes.setLayoutParams(listLp);
        appearanceBackground(containerPurposes, wide ? R.drawable.cmp_bg_box : 0);
        LinearLayout.LayoutParams toggleLp =
                (LinearLayout.LayoutParams) btnToggleList.getLayoutParams();
        toggleLp.topMargin = wide ? dp(12) : 0;
        btnToggleList.setLayoutParams(toggleLp);

        updateFooterVisibility();
        applyAppearance();
        for (PurposeHolder holder : holders) {
            holder.render();
        }
        applyAppearance();
    }

    /**
     * Nguoi dung bat / tat "Dong y tat ca".
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
        int granted = 0;
        for (PurposeHolder holder : holders) {
            holder.render();
            if (state.isGranted(holder.item.key())) {
                granted++;
            }
        }
        applyAppearance();
        swAcceptAll.setChecked(state.isAllGranted());
        binding = false;
        tvSummary.setText(getContext().getString(
                R.string.cmp_selected_summary, granted, holders.size()));
        if (stateChangeListener != null) {
            stateChangeListener.onStateChanged(state, hasAllRequired());
        }
    }

    // ==================== Holders ====================

    /** Mot muc dich (accordion) + cac truong du lieu ben trong. Chi lo phan hien thi. */
    private final class PurposeHolder {

        final ConsentItem item;
        final View root;
        final View head;
        final ImageView chevron;
        final TextView subtitle;
        final TextView counter;
        final SwitchMaterial toggle;
        final View body;
        final TextView description;
        final TextView btnShowMore;
        final LinearLayout meta;
        final CmpFlowLayout parties;
        final View fieldsHead;
        final TextView fieldsCount;
        final TextView btnSelectAll;
        final CmpFlowLayout fieldsContainer;
        final List<FieldHolder> fields = new ArrayList<>();
        final boolean hasBody;
        final boolean longDescription;
        boolean descriptionExpanded;

        PurposeHolder(LayoutInflater inflater, ViewGroup parent, final ConsentItem item) {
            this.item = item;
            this.root = inflater.inflate(R.layout.cmp_item_purpose, parent, false);
            this.head = root.findViewById(R.id.cmp_purpose_head);
            this.chevron = root.findViewById(R.id.cmp_iv_chevron);
            this.subtitle = root.findViewById(R.id.cmp_tv_purpose_subtitle);
            this.counter = root.findViewById(R.id.cmp_tv_purpose_counter);
            this.toggle = root.findViewById(R.id.cmp_sw_purpose);
            styleSwitch(toggle);
            this.body = root.findViewById(R.id.cmp_purpose_body);
            this.description = root.findViewById(R.id.cmp_tv_purpose_description);
            this.btnShowMore = root.findViewById(R.id.cmp_btn_show_more);
            this.meta = root.findViewById(R.id.cmp_purpose_meta);
            this.parties = root.findViewById(R.id.cmp_container_parties);
            this.fieldsHead = root.findViewById(R.id.cmp_fields_head);
            this.fieldsCount = root.findViewById(R.id.cmp_tv_fields_count);
            this.btnSelectAll = root.findViewById(R.id.cmp_btn_select_all);
            this.fieldsContainer = root.findViewById(R.id.cmp_container_fields);

            TextView label = root.findViewById(R.id.cmp_tv_purpose_label);
            label.setText(item.mustBeGranted()
                    ? item.label + getContext().getString(R.string.cmp_required_mark)
                    : item.label);
            toggle.setContentDescription(item.label);

            boolean hasDescription = item.description != null && item.description.length() > 0;
            if (hasDescription) {
                subtitle.setText(item.description);
                description.setText(item.description);
                description.setVisibility(VISIBLE);
            }
            longDescription = hasDescription && item.description.length() > DESCRIPTION_CLAMP_CHARS;
            btnShowMore.setVisibility(longDescription ? VISIBLE : GONE);

            // Ben nhan du lieu cua ca muc ("Chia se voi").
            List<String> sharedWith = sharedWith(item);
            if (!sharedWith.isEmpty()) {
                TextView caption = new TextView(getContext());
                caption.setText(R.string.cmp_shared_with);
                caption.setTextColor(color(R.color.cmp_text_secondary));
                caption.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimension(R.dimen.cmp_text_chip));
                parties.addView(caption);
                for (String name : sharedWith) {
                    TextView chip = (TextView) inflater.inflate(R.layout.cmp_item_chip, parties, false);
                    chip.setText(name);
                    parties.addView(chip);
                }
                parties.setVisibility(VISIBLE);
            }

            for (ConsentField field : item.dataFields) {
                // /config tra ve ca truong khong duoc chon cho purpose — chi de app biet form nguon
                // thu nhung gi. Khong dung UI cho chung (R16).
                if (!field.display) {
                    continue;
                }
                FieldHolder holder = new FieldHolder(inflater, fieldsContainer, item, field);
                fields.add(holder);
                fieldsContainer.addView(holder.view);
            }
            boolean hasFields = !fields.isEmpty();
            fieldsHead.setVisibility(hasFields ? VISIBLE : GONE);
            fieldsContainer.setVisibility(hasFields ? VISIBLE : GONE);
            hasBody = hasDescription || hasFields || !sharedWith.isEmpty();
            chevron.setVisibility(hasBody ? VISIBLE : INVISIBLE);

            // Toggle van bam duoc ke ca voi muc bat buoc: co bam moi hien duoc ly do
            // (view bi setEnabled(false) khong nhan touch nen khong noi duoc gi cho nguoi dung).
            toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton button, boolean checked) {
                    if (binding) {
                        return;
                    }
                    applyPurpose(checked);
                }
            });
            head.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (hasBody) {
                        setOpen(isOpen() ? null : item.key());
                    }
                }
            });
            btnShowMore.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    descriptionExpanded = !descriptionExpanded;
                    render();
                }
            });
            btnSelectAll.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    // "Chon tat ca" = bat muc (kem moi truong); "Bo chon tat ca" = tat muc.
                    applyPurpose(!allFieldsGranted());
                }
            });
        }

        void applyPurpose(boolean granted) {
            ConsentState.ToggleResult result =
                    state.applyPurposeToggle(config, item.key(), granted);
            setError(result == ConsentState.ToggleResult.BLOCKED_REQUIRED
                    ? requiredMessageFor(item, item.required ? null : item.firstRequiredField())
                    : null);
            refreshFromState();
        }

        boolean isOpen() {
            return hasBody && item.key().equals(openKey);
        }

        int grantedFields() {
            int count = 0;
            for (FieldHolder field : fields) {
                if (state.isFieldGranted(field.purposeKey, field.fieldKey)) {
                    count++;
                }
            }
            return count;
        }

        boolean allFieldsGranted() {
            return !fields.isEmpty() && grantedFields() == fields.size();
        }

        /** Ve lai theo state + che do hep/rong + dang mo hay dong. */
        void render() {
            boolean open = isOpen();
            TextView label = root.findViewById(R.id.cmp_tv_purpose_label);
            textSize(label, R.dimen.cmp_text_purpose, R.dimen.cmp_text_purpose_compact);
            for (TextView text : new TextView[]{subtitle, counter, description, btnShowMore,
                    fieldsCount, btnSelectAll}) {
                textSize(text, R.dimen.cmp_text_small, R.dimen.cmp_text_small_compact);
            }
            for (int i = 0; i < parties.getChildCount(); i++) {
                textSize((TextView) parties.getChildAt(i),
                        R.dimen.cmp_text_chip, R.dimen.cmp_text_chip_compact);
            }
            ViewGroup.LayoutParams chevronLp = chevron.getLayoutParams();
            chevronLp.width = dp(compact ? 32 : 40);
            chevron.setLayoutParams(chevronLp);
            subtitle.setMaxLines(compact ? 2 : 1);
            boolean wasBinding = binding;
            binding = true;
            toggle.setChecked(state.isGranted(item.key()));
            binding = wasBinding;

            body.setVisibility(open ? VISIBLE : GONE);
            // Man hep: phan mo rong thang le the (thiet ke mobile); man rong: thang hang ten muc.
            body.setPaddingRelative(wide ? dp(40) : dp(16), 0, wide ? dp(20) : dp(16), dp(14));
            head.setContentDescription(item.label);
            chevron.setRotation(open ? 90 : 0);
            if (open) {
                chevron.setColorFilter(color(R.color.cmp_primary));
            } else {
                chevron.clearColorFilter();
            }
            subtitle.setVisibility(!open && item.description != null
                    && item.description.length() > 0 ? VISIBLE : GONE);
            // Hang dang mo: nen xam nhat o man hep; man rong nen trang trong khung vien.
            root.setBackgroundColor(open && !wide ? color(R.color.cmp_surface) : 0);

            description.setMaxLines(longDescription && !descriptionExpanded
                    ? DESCRIPTION_CLAMP_LINES : Integer.MAX_VALUE);
            btnShowMore.setText(descriptionExpanded ? R.string.cmp_collapse : R.string.cmp_show_more);

            int granted = grantedFields();
            String countText = getContext().getString(R.string.cmp_counter, granted, fields.size());
            counter.setText(countText);
            counter.setVisibility(open && wide && !fields.isEmpty() ? VISIBLE : GONE);
            fieldsCount.setText(getContext().getString(
                    R.string.cmp_fields_count, granted, fields.size()));
            fieldsCount.setVisibility(wide ? GONE : VISIBLE);
            btnSelectAll.setText(allFieldsGranted() ? R.string.cmp_deselect_all : R.string.cmp_select_all);

            // Man rong: "Chia se voi" va "Chon tat ca" cung 1 hang.
            meta.setOrientation(wide ? HORIZONTAL : VERTICAL);
            LinearLayout.LayoutParams partiesLp = (LinearLayout.LayoutParams) parties.getLayoutParams();
            partiesLp.width = wide ? 0 : ViewGroup.LayoutParams.MATCH_PARENT;
            partiesLp.weight = wide ? 1 : 0;
            partiesLp.topMargin = dp(8);
            parties.setLayoutParams(partiesLp);
            LinearLayout.LayoutParams headLp = (LinearLayout.LayoutParams) fieldsHead.getLayoutParams();
            headLp.width = wide ? ViewGroup.LayoutParams.WRAP_CONTENT : ViewGroup.LayoutParams.MATCH_PARENT;
            headLp.topMargin = wide ? dp(8) : dp(4);
            fieldsHead.setLayoutParams(headLp);

            // Truong du lieu: chip tu xuong dong (hep) / checkbox 2 cot trong khung vien (rong).
            fieldsContainer.setColumns(wide ? 2 : 0);
            fieldsContainer.setGaps(wide ? dp(16) : dp(8), wide ? dp(2) : dp(8));
            appearanceBackground(fieldsContainer, wide ? R.drawable.cmp_bg_box : 0);
            fieldsContainer.setPadding(wide ? dp(14) : 0, wide ? dp(6) : 0,
                    wide ? dp(14) : 0, wide ? dp(6) : 0);
            LinearLayout.LayoutParams fieldsLp =
                    (LinearLayout.LayoutParams) fieldsContainer.getLayoutParams();
            fieldsLp.topMargin = wide ? dp(10) : dp(4);
            fieldsContainer.setLayoutParams(fieldsLp);
            for (FieldHolder field : fields) {
                field.render();
            }
        }

        void setEnabled(boolean enabled) {
            toggle.setEnabled(enabled);
            btnSelectAll.setEnabled(enabled);
            for (FieldHolder field : fields) {
                field.view.setEnabled(enabled);
            }
        }
    }

    /** Ben nhan du lieu cua muc; {@link #thirdPartiesOnSharedOnly} thi chi khi co truong chia se. */
    private List<String> sharedWith(ConsentItem item) {
        if (thirdPartiesOnSharedOnly) {
            boolean shared = false;
            for (ConsentField field : item.dataFields) {
                if (field.display && field.sharedWithSystem) {
                    shared = true;
                    break;
                }
            }
            if (!shared) {
                return new ArrayList<>();
            }
        }
        Set<String> names = new LinkedHashSet<>();
        for (String name : item.thirdParties) {
            if (name != null && name.trim().length() > 0) {
                names.add(name.trim());
            }
        }
        return new ArrayList<>(names);
    }

    /** Mot truong du lieu: chip (man hep) / checkbox (man rong). Chi lo phan hien thi. */
    private final class FieldHolder {

        final CheckedTextView view;
        final String purposeKey;
        final String fieldKey;

        FieldHolder(LayoutInflater inflater, ViewGroup parent, final ConsentItem item,
                final ConsentField field) {
            this.view = (CheckedTextView) inflater.inflate(R.layout.cmp_item_field, parent, false);
            this.purposeKey = item.key();
            this.fieldKey = field.key();
            view.setText(field.required
                    ? field.displayName() + getContext().getString(R.string.cmp_required_mark)
                    : field.displayName());
            view.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean granted = !state.isFieldGranted(purposeKey, fieldKey);
                    ConsentState.ToggleResult result =
                            state.applyFieldToggle(config, purposeKey, fieldKey, granted);
                    setError(result == ConsentState.ToggleResult.BLOCKED_REQUIRED
                            ? requiredMessageFor(item, field) : null);
                    refreshFromState();
                }
            });
        }

        void render() {
            boolean checked = state.isFieldGranted(purposeKey, fieldKey);
            textSize(view, R.dimen.cmp_text_body, R.dimen.cmp_text_body_compact);
            view.setChecked(checked);
            if (wide) {
                view.setBackground(null);
                view.setPadding(0, 0, 0, 0);
                view.setMinHeight(dp(40));
                view.setCompoundDrawablePadding(dp(10));
                view.setTextColor(color(R.color.cmp_text_primary));
                view.setTypeface(Typeface.DEFAULT);
                view.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        checked ? R.drawable.cmp_ic_checkbox_on : R.drawable.cmp_ic_checkbox_off,
                        0, 0, 0);
            } else {
                appearanceBackground(view, R.drawable.cmp_bg_field_chip);
                view.setPadding(dp(compact ? 10 : 14), dp(4), dp(compact ? 10 : 14), dp(4));
                view.setMinHeight(dp(36));
                view.setCompoundDrawablePadding(dp(6));
                view.setTextColor(color(checked ? R.color.cmp_primary_pressed : R.color.cmp_text_primary));
                view.setTypeface(checked ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
                view.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        checked ? R.drawable.cmp_ic_check : 0, 0, 0, 0);
            }
        }
    }
}
