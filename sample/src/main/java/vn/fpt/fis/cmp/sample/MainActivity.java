package vn.fpt.fis.cmp.sample;

import vn.fpt.fis.cmp.consent.ConsentAppearance;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import vn.fpt.fis.cmp.consent.ConsentCallback;
import vn.fpt.fis.cmp.consent.ConsentCmp;
import vn.fpt.fis.cmp.consent.ConsentException;
import vn.fpt.fis.cmp.consent.ConsentListener;
import vn.fpt.fis.cmp.consent.ConsentState;
import vn.fpt.fis.cmp.consent.MapValueSource;
import vn.fpt.fis.cmp.consent.model.ConsentConfig;
import vn.fpt.fis.cmp.consent.model.SendConsentResult;
import vn.fpt.fis.cmp.consent.ui.ConsentFormView;

/**
 * App demo cua SDK: nhung {@link ConsentFormView} vao form dang ky (nut gui do app ve), va mo
 * consent dang bottom sheet qua {@link ConsentCmp#show}.
 */
public class MainActivity extends AppCompatActivity {

    private ConsentFormView form;
    private Button btnSubmit;
    private TextView tvResult;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EditText etEmail = findViewById(R.id.et_email);
        EditText etPhone = findViewById(R.id.et_phone);
        form = findViewById(R.id.consent_form);
        btnSubmit = findViewById(R.id.btn_submit);
        tvResult = findViewById(R.id.tv_result);

        if (!ConsentCmp.isInitialized()) {
            tvResult.setText("SDK chưa khởi tạo: khai cmp.codeConfig trong local.properties rồi build lại.");
        }

        // Che do nhung trong form: an nut cua SDK, app tu goi submit.
        form.setEmbeddedInForm(true);
        form.setOnLoadListener(new ConsentFormView.OnLoadListener() {
            @Override
            public void onLoaded(ConsentConfig config) {
                btnSubmit.setEnabled(form.hasAllRequired());
            }

            @Override
            public void onLoadFailed(ConsentException error) {
                tvResult.setText("Tải cấu hình lỗi: " + error.getMessage());
            }
        });
        form.setOnStateChangeListener((state, allRequiredGranted) -> btnSubmit.setEnabled(allRequiredGranted));
        form.setAutoLoad(true);

        btnSubmit.setOnClickListener(v -> {
            // Khoa = name cua truong tren portal CMP.
            MapValueSource values = new MapValueSource()
                    .put("Email", etEmail.getText().toString().trim())
                    .put("PhoneNumber", etPhone.getText().toString().trim());
            form.submit(values, new ConsentCallback<SendConsentResult>() {
                @Override
                public void onSuccess(SendConsentResult result) {
                    tvResult.setText("Đã gửi consent. consentId=" + result.consentId);
                }

                @Override
                public void onError(ConsentException error) {
                    tvResult.setText("Gửi lỗi: " + error.getMessage());
                }
            });
        });

        findViewById(R.id.btn_sheet).setOnClickListener(v -> {
            if (!ConsentCmp.isInitialized()) return;
            ConsentCmp.get().show(getSupportFragmentManager(), new ConsentListener() {
                @Override
                public void onCompleted(ConsentState state, @Nullable SendConsentResult result) {
                    tvResult.setText(result == null
                            ? "Đã có quyết định lưu sẵn trên máy."
                            : "Bottom sheet đã gửi. consentId=" + result.consentId);
                }

                @Override
                public void onError(ConsentException error) {
                    tvResult.setText("Bottom sheet lỗi: " + error.getMessage());
                }
            });
        });

        findViewById(R.id.btn_refresh_appearance).setOnClickListener(v -> {
            if (!ConsentCmp.isInitialized()) return;
            ConsentCmp.get().refreshAppearance(new ConsentCallback<ConsentAppearance>() {
                @Override public void onSuccess(ConsentAppearance appearance) {
                    tvResult.setText("Giao diện: " + appearance.revision);
                    form.reload();
                }
                @Override public void onError(ConsentException error) { tvResult.setText(error.getMessage()); }
            });
        });
        findViewById(R.id.btn_clear_appearance).setOnClickListener(v -> {
            if (!ConsentCmp.isInitialized()) return;
            ConsentCmp.get().clearAppearanceCache();
            form.reload();
        });

        findViewById(R.id.btn_clear).setOnClickListener(v -> {
            if (!ConsentCmp.isInitialized()) return;
            ConsentCmp.get().clear();
            tvResult.setText("Đã xoá quyết định đã lưu.");
        });
    }
}
