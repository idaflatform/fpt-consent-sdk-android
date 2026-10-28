package vn.fpt.fis.cmp.consent;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Cau hinh khoi tao SDK. Tao bang {@link Builder}. */
public final class ConsentOptions {

    /**
     * Dia chi CMP mac dinh (moi truong PROD). App khong khai {@code baseUrl} thi dung gia tri nay;
     * muon tro sang UAT hay moi truong rieng thi goi {@link Builder#baseUrl(String)}.
     */
    public static final String DEFAULT_BASE_URL = "https://cmp.biznext.vn";

    /** Path mac dinh cua nhom API consent tren backend fis-cmp. */
    public static final String DEFAULT_API_PATH = "/api/v1/cmp/consent";
    /** Trang thai gui kem ban ghi consent moi. */
    public static final String DEFAULT_STATUS = "ACTIVE";

    public final String baseUrl;
    public final String codeConfig;
    public final String apiPath;
    public final String status;
    /** Nguon gui ban ghi consent; null = SDK tu suy ra tu app (ten app + package + version). */
    public final String source;
    public final Map<String, String> headers;
    public final int connectTimeoutMs;
    public final int readTimeoutMs;
    public final boolean cacheConfig;
    public final long configTtlMs;

    private ConsentOptions(Builder b) {
        this.baseUrl = stripTrailingSlash(require(b.baseUrl, "baseUrl"));
        this.codeConfig = require(b.codeConfig, "codeConfig");
        this.apiPath = stripTrailingSlash(b.apiPath == null ? DEFAULT_API_PATH : b.apiPath);
        this.status = b.status == null ? DEFAULT_STATUS : b.status;
        this.source = b.source;
        this.headers = Collections.unmodifiableMap(new LinkedHashMap<>(b.headers));
        this.connectTimeoutMs = b.connectTimeoutMs;
        this.readTimeoutMs = b.readTimeoutMs;
        this.cacheConfig = b.cacheConfig;
        this.configTtlMs = b.configTtlMs;
    }

    public String configUrl() {
        return baseUrl + apiPath + "/config?code_config=" + Uris.encode(codeConfig);
    }

    public String sendDataUrl() {
        return baseUrl + apiPath + "/sendData";
    }

    private static String require(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    public static final class Builder {

        private String baseUrl = DEFAULT_BASE_URL;
        private String codeConfig;
        private String apiPath = DEFAULT_API_PATH;
        private String status = DEFAULT_STATUS;
        private String source;
        private final Map<String, String> headers = new LinkedHashMap<>();
        private int connectTimeoutMs = 10000;
        private int readTimeoutMs = 15000;
        private boolean cacheConfig = true;
        private long configTtlMs = 15 * 60 * 1000L;

        /**
         * Ghi de dia chi CMP — chi can khi khong dung PROD.
         * Mac dinh {@value #DEFAULT_BASE_URL}. Khong kem path, vd {@code https://uat-cmp.biznext.vn}.
         */
        public Builder baseUrl(String value) {
            // Truyen null / rong (vd BuildConfig chua khai) thi giu mac dinh thay vi lam hong cau hinh.
            this.baseUrl = value == null || value.trim().isEmpty() ? DEFAULT_BASE_URL : value;
            return this;
        }

        /**
         * {@code code_config} = integration key cua Collection Point.
         *
         * <p>Day la tham so duy nhat can khai bao ngoai {@code baseUrl}: backend tu suy ra
         * tenant / domain / template tu integration key nay, nen SDK khong gui tenant id.</p>
         */
        public Builder codeConfig(String value) {
            this.codeConfig = value;
            return this;
        }

        /** Doi path neu gateway dat prefix khac (mac dinh {@value #DEFAULT_API_PATH}). */
        public Builder apiPath(String value) {
            this.apiPath = value;
            return this;
        }

        /** Trang thai ban ghi khi submit (mac dinh {@value #DEFAULT_STATUS}). */
        public Builder status(String value) {
            this.status = value;
            return this;
        }

        /**
         * Ghi de nguon gui ban ghi consent (cot `source` trong bao cao).
         *
         * <p>Khong khai thi SDK tu dung ten app + package + version, vd
         * {@code "CMP Consent Sample (vn.fpt.fis.cmp.sample) 1.0"}.</p>
         */
        public Builder source(String value) {
            this.source = value;
            return this;
        }

        /** Them header tuy y cho ca 2 request (vd api key). */
        public Builder header(String name, String value) {
            if (name != null && value != null) {
                this.headers.put(name, value);
            }
            return this;
        }

        public Builder timeouts(int connectMs, int readMs) {
            this.connectTimeoutMs = connectMs;
            this.readTimeoutMs = readMs;
            return this;
        }

        /**
         * Cache {@code /config} vao SharedPreferences lam **phuong an du phong khi mat mang**.
         *
         * <p>Mac dinh SDK luon goi mang moi lan tai cau hinh, nen cache khong lam app hien du lieu cu.
         * {@code ttlMs} chi anh huong khi app chu dong goi
         * {@code fetchConfig(true, callback)} de uu tien cache.</p>
         */
        public Builder cacheConfig(boolean enabled, long ttlMs) {
            this.cacheConfig = enabled;
            this.configTtlMs = ttlMs;
            return this;
        }

        public ConsentOptions build() {
            return new ConsentOptions(this);
        }
    }
}
