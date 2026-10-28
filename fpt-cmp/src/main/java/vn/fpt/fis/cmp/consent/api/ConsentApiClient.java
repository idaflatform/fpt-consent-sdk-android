package vn.fpt.fis.cmp.consent.api;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Map;

import vn.fpt.fis.cmp.consent.ConsentException;
import vn.fpt.fis.cmp.consent.ConsentOptions;
import vn.fpt.fis.cmp.consent.model.ConsentConfig;
import vn.fpt.fis.cmp.consent.model.SendConsentResult;

/**
 * HTTP client cua SDK, dung {@link HttpURLConnection} de khong keo them dependency.
 *
 * <p>Moi ham deu chay dong bo (blocking) — {@code ConsentCmp} chiu trach nhiem day sang
 * background thread va tra callback ve main thread.</p>
 */
public final class ConsentApiClient {

    private static final Charset UTF8 = Charset.forName("UTF-8");
    /** Header mang integration key (= {@code code_config}) cho ca 2 request. */
    public static final String HEADER_INTEGRATION = "X-Consent-Integration";

    private final ConsentOptions options;

    public ConsentApiClient(ConsentOptions options) {
        this.options = options;
    }

    /** GET /config?code_config=... */
    public ConsentConfig fetchConfig() throws ConsentException {
        return ConsentJson.parseConfig(fetchConfigJson());
    }

    /** Nhu {@link #fetchConfig()} nhung tra JSON tho de caller cache lai nguyen ban. */
    public JSONObject fetchConfigJson() throws ConsentException {
        return request("GET", options.configUrl(), null);
    }

    /** POST /sendData */
    public SendConsentResult sendData(JSONObject body) throws ConsentException {
        JSONObject json = request("POST", options.sendDataUrl(), body);
        return ConsentJson.parseSendResult(json);
    }

    private JSONObject request(String method, String url, JSONObject body) throws ConsentException {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(options.connectTimeoutMs);
            conn.setReadTimeout(options.readTimeoutMs);
            conn.setRequestProperty("Accept", "application/json");
            // Cau hinh consent doi tren portal la phai thay ngay: khong cho HttpURLConnection,
            // webview cache hay proxy trung gian tra ban cu.
            conn.setUseCaches(false);
            conn.setRequestProperty("Cache-Control", "no-cache, no-store");
            conn.setRequestProperty("Pragma", "no-cache");
            // Khong gui tenant id: backend suy ra tenant tu integration key nay.
            conn.setRequestProperty(HEADER_INTEGRATION, options.codeConfig);
            // Header do app khai bao duoc dat sau cung nen co the ghi de mac dinh o tren.
            for (Map.Entry<String, String> header : options.headers.entrySet()) {
                conn.setRequestProperty(header.getKey(), header.getValue());
            }

            if (body != null) {
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                byte[] payload = body.toString().getBytes(UTF8);
                conn.setFixedLengthStreamingMode(payload.length);
                OutputStream out = conn.getOutputStream();
                try {
                    out.write(payload);
                    out.flush();
                } finally {
                    close(out);
                }
            }

            int status = conn.getResponseCode();
            String text = readAll(status >= 400 ? conn.getErrorStream() : conn.getInputStream());
            if (status >= 400) {
                throw toError(status, text);
            }
            if (text == null || text.trim().isEmpty()) {
                return new JSONObject();
            }
            return new JSONObject(text);
        } catch (ConsentException e) {
            throw e;
        } catch (JSONException e) {
            throw new ConsentException("Response khong phai JSON hop le", e);
        } catch (IOException e) {
            throw new ConsentException("Khong ket noi duoc toi may chu consent", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /** Doc {@code BaseResponse.code/message} tu body loi de bao dung nguyen nhan nghiep vu. */
    private ConsentException toError(int status, String text) {
        String message = "HTTP " + status;
        String errorCode = null;
        if (text != null && !text.trim().isEmpty()) {
            try {
                JSONObject json = new JSONObject(text);
                errorCode = ConsentJson.optString(json, "code");
                String serverMessage = ConsentJson.optString(json, "message");
                if (serverMessage != null) {
                    message = serverMessage;
                }
            } catch (JSONException ignored) {
                // Giu message mac dinh khi body khong phai JSON.
            }
        }
        return new ConsentException(message, status, errorCode, null);
    }

    private String readAll(InputStream in) throws IOException {
        if (in == null) {
            return null;
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, UTF8));
        try {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } finally {
            close(reader);
        }
    }

    private void close(java.io.Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException ignored) {
            // Dong stream that bai khong anh huong ket qua request.
        }
    }
}
