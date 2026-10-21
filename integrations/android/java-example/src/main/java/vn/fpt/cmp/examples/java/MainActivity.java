package vn.fpt.cmp.examples.java;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import vn.fpt.fis.cmp.consent.*;
import vn.fpt.fis.cmp.consent.model.*;
import vn.fpt.fis.cmp.consent.ui.ConsentFormView;

public final class MainActivity extends AppCompatActivity {
    private ConsentFormView form;
    private TextView result;
    private Button submit;
    private boolean ready;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        EditText email=findViewById(R.id.email),phone=findViewById(R.id.phone);
        result=findViewById(R.id.result);form=findViewById(R.id.consent_form);submit=findViewById(R.id.submit);
        if (!ConsentCmp.isInitialized()) {
            result.setText("Chưa cấu hình. Build với -PcmpCodeConfig và -PcmpBaseUrl.");
            findViewById(R.id.standalone).setEnabled(false);findViewById(R.id.clear).setEnabled(false);return;
        }
        form.setOnLoadListener(new ConsentFormView.OnLoadListener() {
            @Override public void onLoaded(ConsentConfig config) { ready=config.isActive(); submit.setEnabled(ready && form.hasAllRequired()); result.setText(ready?"Form đã sẵn sàng":"Collection Point không hoạt động"); }
            @Override public void onLoadFailed(ConsentException error) { ready=false; submit.setEnabled(false); result.setText(error.getMessage()); }
        });
        form.setOnStateChangeListener((s, required)->submit.setEnabled(ready && required && !form.isSubmitting()));
        submit.setOnClickListener(v->{
            if(!ready || form.isSubmitting())return;
            submit.setEnabled(false);
            form.submit(new MapValueSource().put("Email",email.getText().toString()).put("PhoneNumber",phone.getText().toString()),new ConsentCallback<SendConsentResult>() {
                @Override public void onSuccess(SendConsentResult sent) { if(isFinishing()||isDestroyed())return; result.setText("Đã gửi consentId="+sent.consentId+". Tiếp tục nghiệp vụ tại đây."); submit.setEnabled(true); }
                @Override public void onError(ConsentException e) { if(isFinishing()||isDestroyed())return; result.setText(e.getMessage()); submit.setEnabled(ready && form.hasAllRequired()); }
            });
        });
        Button standalone=findViewById(R.id.standalone);
        standalone.setOnClickListener(v->{
            ConsentCmp.get().setValueSource(new MapValueSource().put("Email",email.getText().toString()).put("PhoneNumber",phone.getText().toString()));
            ConsentCmp.get().show(getSupportFragmentManager(),new ConsentListener(){
                @Override public void onCompleted(ConsentState s,SendConsentResult sent){result.setText("Đã lưu consentId="+s.getConsentId());}
                @Override public void onDismissed(){result.setText("Đã đóng; không suy ra đồng ý.");}
                @Override public void onError(ConsentException e){result.setText(e.getMessage());}
            });
        });
        Button clear=findViewById(R.id.clear);
        clear.setOnClickListener(v->{if(form.isSubmitting())return;ConsentCmp.get().clear();ready=false;submit.setEnabled(false);form.reload();result.setText("Đã xóa quyết định và giá trị người dùng.");});
        form.setAutoLoad(true);
    }
}
