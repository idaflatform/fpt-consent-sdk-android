package vn.fpt.fis.cmp.consent;

import android.content.Context;
import android.os.Looper;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import vn.fpt.fis.cmp.consent.model.ConsentConfig;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
public class ConfigAppearanceTest {
    private ServerSocket server;
    private Thread worker;
    private volatile boolean running;
    private volatile String response;
    private final List<String> paths = Collections.synchronizedList(new ArrayList<>());
    private ConsentCmp cmp;

    @Before public void start() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        context.getSharedPreferences("vn.fpt.fis.cmp.appearance", Context.MODE_PRIVATE).edit().clear().commit();
        server = new ServerSocket(0);
        running = true;
        response = config("one").toString();
        worker = new Thread(() -> {
            while (running) try (Socket socket = server.accept()) {
                BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                String request = input.readLine();
                String path = request.split(" ")[1]; paths.add(path);
                String line; while ((line = input.readLine()) != null && !line.isEmpty()) { }
                boolean valid = path.startsWith("/api/v1/cmp/consent/config?");
                byte[] body = (valid ? response : "{}").getBytes(StandardCharsets.UTF_8);
                OutputStream out = socket.getOutputStream();
                out.write(("HTTP/1.1 " + (valid ? "200 OK" : "404 Not Found") + "\r\nContent-Type: application/json\r\nContent-Length: " + body.length + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                out.write(body); out.flush();
            } catch (Exception error) { if (running) throw new AssertionError(error); }
        });
        worker.setDaemon(true); worker.start();
        cmp = ConsentCmp.init(context, new ConsentOptions.Builder().baseUrl("http://127.0.0.1:" + server.getLocalPort())
                .codeConfig("cp-http-test").timeouts(2000, 2000).build());
        cmp.store().clearConfigCache();
    }
    @After public void stop() throws Exception { running = false; server.close(); worker.join(2000); }

    private JSONObject config(String revision) throws Exception {
        JSONObject data = new JSONObject("{\"code\":\"cp-http-test\",\"status\":\"active\",\"fields\":[],\"config\":{\"items\":[]}}");
        if (revision != null) data.put("appearance", new JSONObject("{\"schemaVersion\":1,\"theme\":{},\"typography\":{\"family\":\"system\"},\"revision\":\"" + revision + "\"}"));
        return new JSONObject().put("data", data);
    }
    private <T> T await(Consumer<ConsentCallback<T>> operation) throws Exception {
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<ConsentException> failure = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        operation.accept(new ConsentCallback<T>() {
            public void onSuccess(T value) { result.set(value); done.countDown(); }
            public void onError(ConsentException error) { failure.set(error); done.countDown(); }
        });
        long deadline = System.currentTimeMillis() + 5000;
        while (done.getCount() != 0 && System.currentTimeMillis() < deadline) {
            Shadows.shadowOf(Looper.getMainLooper()).idle(); Thread.sleep(5);
        }
        assertEquals("callback completed", 0, done.getCount());
        if (failure.get() != null) throw failure.get();
        return result.get();
    }
    @Test public void contentAndAppearanceShareOneRequestAndRefreshIsExplicit() throws Exception {
        ConsentConfig loaded = this.<ConsentConfig>await(cb -> cmp.fetchConfig(cb));
        assertNotNull(loaded); assertEquals("one", cmp.appearance().revision); assertEquals(1, paths.size());
        response = config("two").toString();
        this.<ConsentConfig>await(cb -> cmp.fetchConfig(cb));
        assertEquals("one", cmp.appearance().revision); assertEquals(2, paths.size());
        ConsentAppearance refreshed = this.<ConsentAppearance>await(cb -> cmp.refreshAppearance(cb));
        assertEquals("two", refreshed.revision); assertEquals(3, paths.size());
        this.<ConsentAppearance>await(cb -> cmp.getAppearance(cb));
        assertEquals(3, paths.size());
        for (String path : paths) assertTrue(path.startsWith("/api/v1/cmp/consent/config?"));
    }
    @Test public void cachedContentCanRestoreAppearanceWithoutNetwork() throws Exception {
        this.<ConsentConfig>await(cb -> cmp.fetchConfig(cb));
        cmp.clearAppearanceCache();
        this.<ConsentConfig>await(cb -> cmp.fetchConfig(true, cb));
        assertEquals("one", cmp.appearance().revision); assertEquals(1, paths.size());
    }
    @Test public void absentOrInvalidAppearanceDoesNotFailContent() throws Exception {
        response = config(null).toString();
        assertNotNull(this.<ConsentConfig>await(cb -> cmp.fetchConfig(cb)));
        assertEquals("default", cmp.appearance().revision); assertEquals(1, paths.size());
        cmp.clearAppearanceCache();
        response = config(null).getJSONObject("data").put("appearance", new JSONObject().put("schemaVersion", 99)).toString();
        assertNotNull(this.<ConsentConfig>await(cb -> cmp.fetchConfig(cb)));
        assertEquals("default", cmp.appearance().revision); assertEquals(2, paths.size());
    }
    @Test public void standaloneAppearanceLoadsConfigAndPersistsAcrossSdkRestart() throws Exception {
        ConsentAppearance loaded = this.<ConsentAppearance>await(cb -> cmp.getAppearance(cb));
        assertEquals("one", loaded.revision); assertEquals(1, paths.size());
        ConsentOptions options = cmp.options();
        cmp = ConsentCmp.init(RuntimeEnvironment.getApplication(), options);
        this.<ConsentAppearance>await(cb -> cmp.getAppearance(cb));
        assertEquals("one", cmp.appearance().revision); assertEquals(1, paths.size());
    }
}
