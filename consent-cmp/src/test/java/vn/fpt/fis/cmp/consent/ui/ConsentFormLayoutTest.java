package vn.fpt.fis.cmp.consent.ui;

import vn.fpt.fis.cmp.consent.ConsentAppearance;
import org.json.JSONObject;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.io.File;
import java.io.FileOutputStream;

import java.util.Arrays;
import java.util.Collections;

import vn.fpt.fis.cmp.consent.R;
import vn.fpt.fis.cmp.consent.model.ConsentConfig;
import vn.fpt.fis.cmp.consent.model.ConsentField;
import vn.fpt.fis.cmp.consent.model.ConsentItem;
import vn.fpt.fis.cmp.consent.model.ConsentUiConfig;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, qualifiers = "w360dp-h800dp-xhdpi", manifest = Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class ConsentFormLayoutTest {
    private ConsentFormView form(float fontScale) {
        Context app = RuntimeEnvironment.getApplication();
        Configuration configuration = new Configuration(app.getResources().getConfiguration());
        configuration.fontScale = fontScale;
        ConsentFormView form = new ConsentFormView(app.createConfigurationContext(configuration));
        ConsentField field = new ConsentField("email", "Địa chỉ email liên hệ đăng ký bảo hiểm",
                null, "string", false, false, false);
        ConsentItem item = new ConsentItem("purpose", "purpose",
                "Đăng ký bảo hiểm và nhận thông tin về dịch vụ liên quan", "Mô tả mục đích",
                false, true, null, null, null, Arrays.asList(field),
                Arrays.asList("Công ty bảo hiểm và dịch vụ tài chính"));
        form.bind(new ConsentConfig("test", "test", "active", null,
                new ConsentUiConfig("Danh sách mục đích xử lý dữ liệu", null, null,
                        Collections.singletonList(item))), null);
        return form;
    }

    private void layout(ConsentFormView form, int width) {
        form.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        form.layout(0, 0, width, form.getMeasuredHeight());
    }

    @Test public void remoteThemePreservesWrappingAndFontScaleAcrossLayouts() throws Exception {
        ConsentFormView form = form(1.3f);
        form.setAppearance(ConsentAppearance.parse(new JSONObject(
            "{\"schemaVersion\":1,\"theme\":{\"colors\":{\"textPrimary\":\"#442288\",\"primary\":\"#673AB7\"}},\"typography\":{\"family\":\"serif\",\"title\":{\"size\":24},\"body\":{\"size\":18}}}")));
        for (int width : new int[]{624, 1200}) {
            layout(form, width);
            TextView title = form.findViewById(R.id.cmp_tv_title);
            assertEquals(0xff442288, title.getCurrentTextColor());
            assertEquals(24 * form.getResources().getDisplayMetrics().scaledDensity, title.getTextSize(), 0.1f);
            assertCompleteText((TextView) form.findViewById(R.id.cmp_tv_purpose_label));
            ViewGroup fields = form.findViewById(R.id.cmp_container_fields);
            assertCompleteText((TextView) fields.getChildAt(0));
        }
    }

    @Test public void narrow720PixelHostWrapsAllPurposeAndFieldText() throws Exception {
        for (float scale : new float[]{1f, 1.3f}) {
            ConsentFormView form = form(scale);
            // 720 physical pixels, density 2, host padding 24dp on each side.
            layout(form, 624);
            TextView label = form.findViewById(R.id.cmp_tv_purpose_label);
            assertTrue(label.getLineCount() > 1);
            assertCompleteText(label);
            ViewGroup fields = form.findViewById(R.id.cmp_container_fields);
            TextView field = (TextView) fields.getChildAt(0);
            assertCompleteText(field);
            assertTrue(field.getRight() <= fields.getWidth());
            assertTrue(field.getBottom() <= fields.getHeight());
            Bitmap preview = Bitmap.createBitmap(720, 1600, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(preview);
            canvas.drawColor(Color.WHITE);
            canvas.translate(48, 96);
            form.draw(canvas);
            File folder = new File("build/reports/layout");
            assertTrue(folder.isDirectory() || folder.mkdirs());
            try (FileOutputStream output = new FileOutputStream(new File(folder,
                    "consent-720x1600-font-" + scale + ".png"))) {
                assertTrue(preview.compress(Bitmap.CompressFormat.PNG, 100, output));
            }
            preview.recycle();
        }
    }

    @Test public void firstMeasureUsesCompactTypographyAndResizeRestoresRegularSize() {
        ConsentFormView form = form(1f);
        layout(form, 624);
        TextView title = form.findViewById(R.id.cmp_tv_title);
        assertEquals(form.getResources().getDimension(R.dimen.cmp_text_title_compact),
                title.getTextSize(), 0.01f);
        layout(form, 800);
        assertEquals(form.getResources().getDimension(R.dimen.cmp_text_title),
                title.getTextSize(), 0.01f);
        layout(form, 624);
        assertEquals(form.getResources().getDimension(R.dimen.cmp_text_title_compact),
                title.getTextSize(), 0.01f);
    }

    @Test public void expandingEmbeddedListRequiresMoreHostHeight() {
        ConsentFormView form = form(1f);
        form.setEmbeddedInForm(true);
        layout(form, 624);
        int collapsedHeight = form.getMeasuredHeight();
        form.findViewById(R.id.cmp_btn_toggle_list).performClick();
        layout(form, 624);
        assertTrue(form.getMeasuredHeight() > collapsedHeight);
        TextView label = form.findViewById(R.id.cmp_tv_purpose_label);
        assertCompleteText(label);
    }

    private void assertCompleteText(TextView text) {
        assertNotNull(text.getLayout());
        int last = text.getLayout().getLineCount() - 1;
        assertEquals(0, text.getLayout().getEllipsisCount(last));
        assertEquals(text.getText().length(), text.getLayout().getLineEnd(last));
        assertTrue(text.getHeight() >= text.getLayout().getHeight()
                + text.getCompoundPaddingTop() + text.getCompoundPaddingBottom());
    }
}
