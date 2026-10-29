package vn.fpt.fis.cmp.consent;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.SystemClock;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import vn.fpt.fis.cmp.consent.api.ConsentApiClient;
import vn.fpt.fis.cmp.consent.api.ConsentJson;

/** Persisted fallback, updated from each successful config response. Runs off the UI thread. */
@SuppressLint("ApplySharedPref") // Calls run on the SDK worker; durability is required before restart.
public final class AppearanceRepository {
    public interface Loader { JSONObject load() throws Exception; }
    private final SharedPreferences prefs;
    private final String key;
    private final File fonts;
    private final Loader loader;
    private final Map<String, Typeface> typefaces = new ConcurrentHashMap<>();
    private ConsentAppearance current;
    private boolean attempted;

    public AppearanceRepository(Context context, ConsentOptions options, ConsentApiClient api) {
        this(context, options, () -> ConsentJson.appearanceFromConfig(api.fetchConfigJson()));
    }
    public AppearanceRepository(Context context, ConsentOptions options, Loader loader) {
        prefs = context.getSharedPreferences("vn.fpt.fis.cmp.appearance", Context.MODE_PRIVATE);
        key = hash(options.baseUrl.replaceAll("/+$", "")
                + "|" + options.apiPath + "|" + options.codeConfig + "|1");
        fonts = new File(context.getFilesDir(), "cmp-appearance-fonts");
        this.loader = loader;
    }
    private ConsentAppearance cached() {
        if (current != null) return current;
        String raw = prefs.getString(key, null);
        if (raw != null) try {
            current = ConsentAppearance.parse(new JSONObject(raw)); return current;
        } catch (Exception ignored) { prefs.edit().remove(key).commit(); }
        return null;
    }
    public synchronized ConsentAppearance cachedOrDefault() {
        ConsentAppearance cached = cached();
        return cached == null ? ConsentAppearance.defaults() : cached;
    }
    public synchronized ConsentAppearance get() {
        ConsentAppearance cached = cached();
        if (cached != null) return cached;
        if (attempted) return ConsentAppearance.defaults();
        attempted = true;
        return fetch(false, loader);
    }
    /** Reuse the content request response; this path never makes another config request. */
    public synchronized ConsentAppearance fromConfig(JSONObject config) {
        attempted = true;
        return fetch(false, () -> ConsentJson.appearanceFromConfig(config));
    }
    /** Stored content must not replace a newer valid theme cache. */
    synchronized ConsentAppearance fromCachedConfig(JSONObject config) {
        ConsentAppearance existing = cached();
        if (existing != null && existing.json().optInt("schemaVersion") == 1) return existing;
        return fromConfig(config);
    }
    public synchronized ConsentAppearance refresh() { attempted = true; return fetch(true, loader); }
    private ConsentAppearance fetch(boolean refresh, Loader source) {
        ConsentAppearance previous = cached();
        try {
            ConsentAppearance result = ConsentAppearance.parse(source.load());
            if (!refresh && previous != null && sameJson(previous.json(), result.json())) return previous;
            downloadFonts(result);
            prefs.edit().putString(key, result.json().toString()).commit();
            typefaces.clear();
            current = result;
        } catch (Exception ignored) { current = previous == null ? ConsentAppearance.defaults() : previous; }
        return current;
    }
    /** Object key order is irrelevant; font arrays retain their ordering. */
    private static boolean sameJson(Object first, Object second) {
        if (first instanceof JSONObject && second instanceof JSONObject) {
            JSONObject a = (JSONObject) first, b = (JSONObject) second;
            if (a.length() != b.length()) return false;
            Iterator<String> keys = a.keys();
            while (keys.hasNext()) {
                String name = keys.next();
                if (!b.has(name) || !sameJson(a.opt(name), b.opt(name))) return false;
            }
            return true;
        }
        if (first instanceof JSONArray && second instanceof JSONArray) {
            JSONArray a = (JSONArray) first, b = (JSONArray) second;
            if (a.length() != b.length()) return false;
            for (int i = 0; i < a.length(); i++) if (!sameJson(a.opt(i), b.opt(i))) return false;
            return true;
        }
        return first == null ? second == null : first.equals(second);
    }
    public synchronized void clear() {
        prefs.edit().remove(key).commit(); current = null; attempted = false; typefaces.clear();
        // Fonts are content-addressed and may be shared by another Collection Point.
    }
    public Typeface typeface(ConsentAppearance appearance, int weight) {
        String cacheKey = appearance.revision + "|" + appearance.family() + "|" + weight;
        if (typefaces.containsKey(cacheKey)) return typefaces.get(cacheKey);
        JSONArray list = appearance.json().optJSONObject("typography") == null ? null
                : appearance.json().optJSONObject("typography").optJSONArray("fonts");
        int distance = Integer.MAX_VALUE; Typeface best = null;
        if (list != null) for (int i = 0; i < Math.min(4, list.length()); i++) {
            JSONObject font = list.optJSONObject(i);
            if (font == null) continue;
            String digest = font.optString("sha256").toLowerCase(Locale.ROOT);
            if (!digest.matches("[a-f0-9]{64}")) continue;
            int d = Math.abs(font.optInt("weight", 400) - weight);
            File file = new File(fonts, digest + ".font");
            if (d < distance && file.isFile()) try {
                if (!digest.equals(fileHash(file))) continue;
                best = Typeface.createFromFile(file); distance = d;
            } catch (Exception ignored) { }
        }
        if (best == null) {
            String family = appearance.family();
            best = Typeface.create("system".equals(family) ? null : family, weight >= 600 ? Typeface.BOLD : Typeface.NORMAL);
        }
        typefaces.put(cacheKey, best);
        return best;
    }
    private void downloadFonts(ConsentAppearance appearance) {
        JSONObject type = appearance.json().optJSONObject("typography");
        JSONArray list = type == null ? null : type.optJSONArray("fonts");
        if (list == null) return;
        fonts.mkdirs();
        long deadline = SystemClock.elapsedRealtime() + 3000;
        for (int i = 0; i < Math.min(4, list.length()); i++) {
            JSONObject font = list.optJSONObject(i);
            if (font == null) continue;
            String digest = font.optString("sha256").toLowerCase(Locale.ROOT);
            String url = font.optString("url");
            if (!digest.matches("[a-f0-9]{64}") || !url.startsWith("https://")) continue;
            File target = new File(fonts, digest + ".font");
            HttpURLConnection conn = null; File temporary = null;
            try {
                if (target.isFile() && digest.equals(fileHash(target))) continue;
                int remaining = (int) (deadline - SystemClock.elapsedRealtime());
                if (remaining <= 0) break;
                conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setInstanceFollowRedirects(false);
                conn.setConnectTimeout(remaining); conn.setReadTimeout(remaining);
                if (conn.getResponseCode() != 200 || conn.getContentLength() > 4 * 1024 * 1024) continue;
                temporary = File.createTempFile("cmp-font", ".tmp", fonts);
                try (InputStream input = conn.getInputStream(); OutputStream output = new FileOutputStream(temporary)) {
                    byte[] bytes = new byte[8192]; int total = 0, n;
                    while ((n = input.read(bytes)) != -1) {
                        total += n;
                        if (total > 4 * 1024 * 1024 || SystemClock.elapsedRealtime() > deadline) throw new IOException();
                        output.write(bytes, 0, n);
                    }
                }
                if (digest.equals(fileHash(temporary))) {
                    Typeface.createFromFile(temporary); // Reject invalid native font files before caching.
                    if (target.exists()) target.delete();
                    temporary.renameTo(target);
                }
            } catch (Exception ignored) { /* Font errors never discard a usable theme. */ }
            finally { if (conn != null) conn.disconnect(); if (temporary != null) temporary.delete(); }
        }
    }
    private static String fileHash(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = new FileInputStream(file)) { byte[] b = new byte[8192]; int n;
            while ((n = in.read(b)) != -1) digest.update(b, 0, n); }
        return hex(digest.digest());
    }
    private static String hash(String value) {
        try { return hex(MessageDigest.getInstance("SHA-256").digest(value.getBytes("UTF-8"))); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
    private static String hex(byte[] bytes) {
        StringBuilder out = new StringBuilder(); for (byte b : bytes) out.append(String.format("%02x", b & 255)); return out.toString();
    }
}
