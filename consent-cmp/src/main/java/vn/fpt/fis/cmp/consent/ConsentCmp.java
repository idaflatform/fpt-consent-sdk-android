package vn.fpt.fis.cmp.consent;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vn.fpt.fis.cmp.consent.api.ConsentApiClient;
import vn.fpt.fis.cmp.consent.api.ConsentJson;
import vn.fpt.fis.cmp.consent.model.ConsentConfig;
import vn.fpt.fis.cmp.consent.model.ConsentField;
import vn.fpt.fis.cmp.consent.model.ConsentItem;
import vn.fpt.fis.cmp.consent.model.SendConsentResult;
import vn.fpt.fis.cmp.consent.ui.ConsentSheetFragment;

/**
 * Diem vao cua SDK.
 *
 * <pre>{@code
 * ConsentCmp.init(this, new ConsentOptions.Builder()
 *         .baseUrl("https://cmp.example.vn")
 *         .codeConfig("cp_mobile_app")   // tham so duy nhat can khai bao khi nhung SDK
 *         .build());
 *
 * ConsentCmp.get().bindForm(binding.getRoot());   // lay gia tri truong du lieu tu form cua app
 * ConsentCmp.get().showIfNeeded(getSupportFragmentManager(), new ConsentListener() { ... });
 * }</pre>
 */
public final class ConsentCmp {

    private static volatile ConsentCmp instance;

    private final Context appContext;
    private final ConsentOptions options;
    private final ConsentApiClient api;
    private final ConsentStore store;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    private volatile ConsentConfig cachedConfig;
    /** Chuoi mo ta app, tinh mot lan roi dung lai. */
    private volatile String appSource;
    private volatile ConsentValueSource valueSource;
    private ConsentListener listener;

    private ConsentCmp(Context context, ConsentOptions options) {
        this.appContext = context.getApplicationContext();
        this.options = options;
        this.api = new ConsentApiClient(options);
        this.store = new ConsentStore(this.appContext);
    }

    /**
     * Khoi tao SDK bang code; thang cau hinh {@code <meta-data>} (xem {@link ConsentInitProvider}).
     */
    public static ConsentCmp init(Context context, ConsentOptions options) {
        synchronized (ConsentCmp.class) {
            ConsentCmp previous = instance;
            instance = new ConsentCmp(context, options);
            if (previous != null) {
                // Tranh ro thread cua instance cu.
                previous.executor.shutdown();
            }
            return instance;
        }
    }

    /**
     * Khoi tao tu {@code <meta-data>} neu chua khoi tao; tra false khi da co instance.
     */
    static boolean initIfAbsent(Context context, ConsentOptions options) {
        synchronized (ConsentCmp.class) {
            if (instance != null) {
                return false;
            }
            instance = new ConsentCmp(context, options);
            return true;
        }
    }

    public static ConsentCmp get() {
        ConsentCmp local = instance;
        if (local == null) {
            throw new IllegalStateException("ConsentCmp chua duoc khoi tao: khai <meta-data android:name=\""
                    + ConsentInitProvider.META_CODE_CONFIG + "\"> trong AndroidManifest.xml hoac goi "
                    + "ConsentCmp.init(context, options)");
        }
        return local;
    }

    public static boolean isInitialized() {
        return instance != null;
    }

    public ConsentOptions options() {
        return options;
    }

    public ConsentStore store() {
        return store;
    }

    /** Visitor ID gan cho thiet bi, gui kem moi ban ghi consent. */
    public String visitorId() {
        return store.visitorId();
    }

    /**
     * Nguon gui ban ghi consent — di kem field {@code source} cua {@code /sendData}.
     *
     * <p>Mac dinh la ten app + package + version doc tu {@code PackageManager}, vd
     * {@code "CMP Consent Sample (vn.fpt.fis.cmp.sample) 1.0"}. App ghi de bang
     * {@link ConsentOptions.Builder#source(String)}.</p>
     */
    public String source() {
        if (options.source != null && !options.source.trim().isEmpty()) {
            return options.source.trim();
        }
        if (appSource == null) {
            appSource = AppInfo.describe(appContext);
        }
        return appSource;
    }

    // ==================== API ====================

    /**
     * Tai cau hinh form tu {@code GET /config} — **luon goi mang** de bat kip thay doi tren portal.
     *
     * <p>Ban cache chi dung lam phuong an du phong khi mat mang. Neu app can mo UI thuc nhanh va chap
     * nhan du lieu cu, goi {@link #fetchConfig(boolean, ConsentCallback)} voi {@code preferCache = true}.</p>
     */
    public void fetchConfig(final ConsentCallback<ConsentConfig> callback) {
        fetchConfig(false, callback);
    }

    /**
     * @param preferCache true = dung ban cache con han neu co (nhanh nhung co the cu);
     *                    false = luon goi mang, chi rot ve cache khi mat mang
     */
    public void fetchConfig(final boolean preferCache, final ConsentCallback<ConsentConfig> callback) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (preferCache && options.cacheConfig) {
                        String cached = store.loadConfig(options.configTtlMs, System.currentTimeMillis());
                        if (cached != null) {
                            try {
                                ConsentConfig config = ConsentJson.parseConfig(new JSONObject(cached));
                                cachedConfig = config;
                                postSuccess(callback, config);
                                return;
                            } catch (JSONException ignored) {
                                // Chi xoa cache, giu quyet dinh da luu.
                                store.clearConfigCache();
                            }
                        }
                    }
                    JSONObject json = api.fetchConfigJson();
                    ConsentConfig config = ConsentJson.parseConfig(json);
                    if (options.cacheConfig) {
                        store.saveConfig(json.toString(), System.currentTimeMillis());
                    }
                    cachedConfig = config;
                    postSuccess(callback, config);
                } catch (ConsentException e) {
                    // Mat mang: dung tam ban cache cu (neu co) de nguoi dung van tra loi duoc.
                    ConsentConfig fallback = e.httpStatus == 0 ? loadCachedConfig() : null;
                    if (fallback != null) {
                        cachedConfig = fallback;
                        postSuccess(callback, fallback);
                    } else {
                        postError(callback, e);
                    }
                } catch (RuntimeException e) {
                    // Khong de exception thoat khoi executor (se crash app).
                    postError(callback, new ConsentException("Khong tai duoc cau hinh consent", e));
                }
            }
        });
    }

    /** Ban cache cu cua /config, khong xet TTL; null khi chua tung tai duoc lan nao. */
    private ConsentConfig loadCachedConfig() {
        if (!options.cacheConfig) {
            return null;
        }
        String raw = store.loadConfigIgnoringTtl();
        if (raw == null) {
            return null;
        }
        try {
            return ConsentJson.parseConfig(new JSONObject(raw));
        } catch (JSONException e) {
            return null;
        }
    }

    /** Cau hinh da tai gan nhat trong phien nay; null neu chua goi {@link #fetchConfig}. */
    @Nullable
    public ConsentConfig config() {
        return cachedConfig;
    }

    /**
     * Khai bao noi lay gia tri that cua truong du lieu tu form cua app.
     *
     * <p>Truoc khi submit, SDK duyet cac truong co {@code sharedWithSystem = true} trong
     * {@code /config}, doi chieu {@code name} (+ {@code dataType}) voi form cua app va gan
     * {@code value} vao dung truong. Truong khong tim thay chi gui {@code isAccept}.</p>
     *
     * @see MapValueSource
     * @see vn.fpt.fis.cmp.consent.ui.ViewFormValueSource
     */
    public void setValueSource(@Nullable ConsentValueSource source) {
        this.valueSource = source;
    }

    /** Nguon gia tri da khai bang {@link #setValueSource} / {@link #bindForm}; null neu chua khai. */
    @Nullable
    public ConsentValueSource valueSource() {
        return valueSource;
    }

    /** Tien ich: quet truc tiep cay view cua form (input danh dau bang {@code android:tag}). */
    public void bindForm(android.view.View formRoot) {
        setValueSource(new vn.fpt.fis.cmp.consent.ui.ViewFormValueSource(formRoot));
    }

    /**
     * Gui quyet dinh cua nguoi dung len {@code POST /sendData} va luu lai cuc bo khi thanh cong.
     *
     * <p>Purpose bat buoc chua dong y se bi chan ngay tai client (backend cung chan lan hai).
     * Moi lan goi sinh mot {@code consentId} moi; {@code visitorId} thi giu nguyen suot vong doi
     * cai dat app de CMP lien ket cac ban ghi cua cung mot nguoi dung.</p>
     * Dung {@code ConsentFormView} thi goi {@code form.submit(...)}.
     */
    @MainThread
    public void submit(ConsentState state, ConsentCallback<SendConsentResult> callback) {
        submit(state, null, null, callback);
    }

    /**
     * Nhu {@link #submit(ConsentState, ConsentCallback)} nhung chi ro cau hinh va nguon gia tri.
     *
     * @param config null = cau hinh tai gan nhat
     * @param values null = nguon da khai bang {@link #setValueSource}
     */
    @MainThread
    public void submit(final ConsentState state, @Nullable ConsentConfig config,
                       @Nullable ConsentValueSource values,
                       final ConsentCallback<SendConsentResult> callback) {
        ConsentConfig active = config != null ? config : cachedConfig;
        if (active == null) {
            active = loadCachedConfig();
        }
        if (active == null) {
            // Khong co cau hinh thi khong kiem tra duoc bat buoc.
            postError(callback, new ConsentException(appContext.getString(R.string.cmp_error_no_config)));
            return;
        }
        ConsentState.MissingRequired missing = state.firstMissing(active);
        if (missing != null) {
            String message = missing.field == null
                    ? appContext.getString(R.string.cmp_error_required, missing.item.label)
                    : appContext.getString(R.string.cmp_error_field_required,
                            missing.item.label, missing.field.displayName());
            postError(callback, new ConsentException(message));
            return;
        }
        // Chi gui khoa co trong cau hinh dang dung: khoa cua ban cau hinh cu (purpose/truong da bi go)
        // khong duoc phep di kem request moi.
        state.retainOnly(active);
        // Lay gia tri tu form cua app tren main thread (doc View phai o main thread).
        collectValues(state, active, values != null ? values : valueSource);
        // Truong an luon isAccept = false (R16).
        state.applyDisplay(active);
        // Thread nen chi doc ban sao.
        final ConsentState snapshot = state.copy();
        // Moi lan submit la mot ban ghi bang chung moi -> consentId luon sinh moi, khong tai su dung
        // id cua lan truoc (gui lai id cu se bi backend tu choi 409). Chi visitorId la giu nguyen.
        final String consentId = UUID.randomUUID().toString();
        final String dateCreated = Iso8601.now();

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = ConsentJson.buildSendRequest(consentId, options.codeConfig,
                            dateCreated, options.status, store.visitorId(), source(), snapshot.toValues());
                    final SendConsentResult result = api.sendData(body);
                    final String savedId = result.consentId != null ? result.consentId : consentId;
                    final long submittedAt = System.currentTimeMillis();
                    snapshot.setConsentId(savedId);
                    snapshot.setConfigCode(options.codeConfig);
                    snapshot.setSubmittedAtMs(submittedAt);
                    store.saveState(snapshot);
                    main.post(new Runnable() {
                        @Override
                        public void run() {
                            // State goc chi duoc ghi tren main thread.
                            state.setConsentId(savedId);
                            state.setConfigCode(options.codeConfig);
                            state.setSubmittedAtMs(submittedAt);
                            if (callback != null) {
                                callback.onSuccess(result);
                            }
                        }
                    });
                } catch (ConsentException e) {
                    postError(callback, e);
                } catch (JSONException e) {
                    postError(callback, new ConsentException("Khong dung duoc body sendData", e));
                } catch (RuntimeException e) {
                    postError(callback, new ConsentException("Khong gui duoc du lieu consent", e));
                }
            }
        });
    }

    // ==================== UI ====================

    /** Mo man hinh danh sach su dong y (bottom sheet). */
    @MainThread
    public void show(FragmentManager fragmentManager, @Nullable ConsentListener listener) {
        this.listener = listener;
        ConsentSheetFragment.show(fragmentManager);
    }

    /**
     * Chi mo UI khi nguoi dung chua tra loi lan nao; nguoc lai tra ngay quyet dinh da luu.
     *
     * @return true neu UI duoc mo
     */
    @MainThread
    public boolean showIfNeeded(FragmentManager fragmentManager, @Nullable ConsentListener listener) {
        ConsentState saved = store.loadState();
        if (saved != null) {
            if (listener != null) {
                listener.onCompleted(saved, null);
            }
            return false;
        }
        show(fragmentManager, listener);
        return true;
    }

    @Nullable
    public ConsentListener listener() {
        return listener;
    }

    // ==================== Trang thai da luu ====================

    /** Nguoi dung da submit consent it nhat mot lan. */
    public boolean hasConsented() {
        return store.hasConsented();
    }

    @Nullable
    public ConsentState savedState() {
        return store.loadState();
    }

    /** Kiem tra nhanh mot purpose da duoc dong y hay chua (dung truoc khi bat SDK ben thu ba). */
    public boolean isGranted(String purposeKey) {
        ConsentState state = store.loadState();
        return state != null && state.isGranted(purposeKey);
    }

    /**
     * Xoa ban cache cua {@code /config} (ca tren dia va trong bo nho) — giu nguyen quyet dinh da luu.
     *
     * <p>Dung khi app biet cau hinh vua doi tren portal va muon chac chan lan {@code fetchConfig}
     * ke tiep khong the tra ban cu.</p>
     */
    public void invalidateConfigCache() {
        cachedConfig = null;
        store.clearConfigCache();
    }

    /** Xoa quyet dinh + cache config (vd khi nguoi dung dang xuat) — giu visitor ID. */
    public void clear() {
        cachedConfig = null;
        store.clear();
    }

    // ==================== Helpers ====================

    /**
     * Gan gia tri form vao cac truong duoc chia se cho he thong ngoai.
     *
     * <p>Chi truong co {@code sharedWithSystem = true} moi duoc lay gia tri; doi chieu theo
     * {@code name} va {@code dataType} nen form cua app khong can biet id cua truong.</p>
     */
    private void collectValues(ConsentState state, ConsentConfig config, @Nullable ConsentValueSource source) {
        if (source == null || config == null) {
            return;
        }
        for (ConsentItem item : config.items()) {
            for (ConsentField field : item.dataFields) {
                if (!field.collectsValue()) {
                    continue;
                }
                // Lay gia tri cho ca truong dang tat: ban ghi can the hien nguoi dung tu choi du lieu nao.
                String value = source.valueFor(field);
                if (value != null && !value.trim().isEmpty()) {
                    state.setFieldValue(item.key(), field.key(), value);
                }
            }
        }
    }

    private <T> void postSuccess(final ConsentCallback<T> callback, final T value) {
        if (callback == null) {
            return;
        }
        main.post(new Runnable() {
            @Override
            public void run() {
                callback.onSuccess(value);
            }
        });
    }

    private <T> void postError(final ConsentCallback<T> callback, final ConsentException error) {
        if (callback == null) {
            return;
        }
        main.post(new Runnable() {
            @Override
            public void run() {
                callback.onError(error);
            }
        });
    }
}
