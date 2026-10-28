package vn.fpt.fis.cmp.consent;

import android.content.Context;
import org.json.JSONObject;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
public class AppearanceRepositoryTest {
    private Context context;
    private ConsentOptions options(String code) { return new ConsentOptions.Builder().baseUrl("https://cmp.example.test").codeConfig(code).build(); }
    private JSONObject response(String revision) throws Exception {
        return new JSONObject("{\"schemaVersion\":1,\"theme\":{\"colors\":{\"primary\":\"#5522AA\"}},\"typography\":{\"family\":\"system\"},\"revision\":\"" + revision + "\"}");
    }
    @Before public void clear() {
        context = RuntimeEnvironment.getApplication();
        context.getSharedPreferences("vn.fpt.fis.cmp.appearance", Context.MODE_PRIVATE).edit().clear().commit();
    }
    @Test public void successSurvivesNewRepositoryWithoutAnotherRequest() throws Exception {
        AtomicInteger count = new AtomicInteger();
        AppearanceRepository.Loader loader = () -> { count.incrementAndGet(); return response("one"); };
        AppearanceRepository first = new AppearanceRepository(context, options("cp1"), loader);
        assertEquals("one", first.get().revision); first.get();
        AppearanceRepository reopened = new AppearanceRepository(context, options("cp1"), loader);
        assertEquals("one", reopened.get().revision);
        assertEquals(1, count.get());
        assertEquals(0xff5522aa, reopened.get().color("primary", 0));
    }
    @Test public void offlineUsesDefaultOncePerSessionThenRetriesNextSession() throws Exception {
        AtomicInteger count = new AtomicInteger();
        AppearanceRepository failed = new AppearanceRepository(context, options("cp1"), () -> { count.incrementAndGet(); throw new Exception("offline"); });
        assertEquals("default", failed.get().revision); failed.get();
        assertEquals(1, count.get());
        AppearanceRepository nextSession = new AppearanceRepository(context, options("cp1"), () -> { count.incrementAndGet(); return response("online"); });
        assertEquals("online", nextSession.get().revision); assertEquals(2, count.get());
    }
    @Test public void refreshFailureKeepsGoodCacheAndExplicitRefreshUpdatesIt() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        AppearanceRepository repo = new AppearanceRepository(context, options("cp1"), () -> {
            int n = calls.incrementAndGet(); if (n == 2) throw new Exception("offline"); return response("revision" + n);
        });
        assertEquals("revision1", repo.get().revision);
        assertEquals("revision1", repo.refresh().revision);
        assertEquals("revision3", repo.refresh().revision);
        assertEquals("revision3", new AppearanceRepository(context, options("cp1"), () -> { throw new AssertionError(); }).get().revision);
    }
    @Test public void configurationAndEnvironmentHaveIndependentCaches() throws Exception {
        new AppearanceRepository(context, options("cp1"), () -> response("one")).get();
        assertEquals("two", new AppearanceRepository(context, options("cp2"), () -> response("two")).get().revision);
        ConsentOptions uat = new ConsentOptions.Builder().baseUrl("https://uat.example.test").codeConfig("cp1").build();
        assertEquals("uat", new AppearanceRepository(context, uat, () -> response("uat")).get().revision);
    }
    @Test public void concurrentFormsShareOneRequest() throws Exception {
        AtomicInteger count = new AtomicInteger();
        AppearanceRepository repo = new AppearanceRepository(context, options("cp1"), () -> { count.incrementAndGet(); Thread.sleep(50); return response("one"); });
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            Future<?>[] futures = new Future<?>[4];
            for (int i=0; i<4; i++) futures[i] = pool.submit(() -> assertEquals("one", repo.get().revision));
            for (Future<?> f : futures) f.get(3, TimeUnit.SECONDS);
            assertEquals(1, count.get());
        } finally { pool.shutdownNow(); }
    }
    @Test public void invalidSchemaDoesNotPoisonCacheAndClearDoesNotRemoveConsent() throws Exception {
        ConsentStore consent = new ConsentStore(context); String visitor = consent.visitorId();
        AtomicInteger calls = new AtomicInteger();
        AppearanceRepository repo = new AppearanceRepository(context, options("cp1"), () -> {
            calls.incrementAndGet(); return new JSONObject("{\"schemaVersion\":99}");
        });
        assertEquals("default", repo.get().revision); repo.get(); assertEquals(1, calls.get());
        assertEquals("valid", new AppearanceRepository(context, options("cp1"), () -> response("valid")).get().revision);
        repo.clear(); assertEquals(visitor, consent.visitorId());
        assertEquals("fresh", new AppearanceRepository(context, options("cp1"), () -> response("fresh")).get().revision);
    }
    @Test public void invalidIndividualTokensUseDefaults() throws Exception {
        ConsentAppearance theme = ConsentAppearance.parse(new JSONObject("{\"schemaVersion\":1,\"theme\":{\"colors\":{\"primary\":\"oops\"},\"shape\":{\"cardRadius\":999}},\"typography\":{\"family\":\"system\",\"body\":{\"size\":-1}}}"));
        assertEquals(123, theme.color("primary", 123));
        assertEquals(12, theme.radius("cardRadius", 12), 0);
        assertEquals(14, theme.size("body", 14), 0);
    }
}
