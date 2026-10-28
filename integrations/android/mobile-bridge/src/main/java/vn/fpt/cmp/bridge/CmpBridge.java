package vn.fpt.cmp.bridge;

import android.app.Activity;
import android.app.Dialog;
import android.app.Application;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.Window;
import android.widget.*;
import org.json.*;
import java.net.URI;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import vn.fpt.fis.cmp.consent.*;
import vn.fpt.fis.cmp.consent.model.*;
import vn.fpt.fis.cmp.consent.ui.ConsentFormView;

/** Modal facade usable from Java/Kotlin, JNI and .NET bindings. No framework dependency. */
public final class CmpBridge {
 public interface Callback { void onResult(String json); }
 private static volatile boolean active;
 private static volatile boolean inFlight;
 private CmpBridge(){}
 public static void present(Activity activity,String optionsJson,Callback callback){
  if(activity==null){callback.onResult(error("not_ready","An Activity is required."));return;}
  activity.runOnUiThread(()->open(activity,optionsJson,callback));
 }
 private static void open(Activity activity,String json,Callback callback){
  if(active||inFlight){callback.onResult(error("busy","A consent screen or submission is already active."));return;}
  if(activity.isFinishing()||activity.isDestroyed()){callback.onResult(error("disposed","Activity is closed."));return;}
  final JSONObject options;final MapValueSource source=new MapValueSource();
  try {
   options=new JSONObject(json);String key=options.optString("codeConfig","").trim();String url=options.optString("baseUrl","https://cmp.biznext.vn");URI uri=new URI(url);
   if(key.isEmpty()||!("https".equals(uri.getScheme())||"http".equals(uri.getScheme()))||uri.getHost()==null||uri.getUserInfo()!=null||uri.getQuery()!=null||uri.getFragment()!=null||!(uri.getPath().isEmpty()||"/".equals(uri.getPath())))throw new IllegalArgumentException("Expected key and HTTP(S) origin.");
   if(ConsentCmp.isInitialized()&&(!ConsentCmp.get().options().codeConfig.equals(key)||!ConsentCmp.get().options().baseUrl.equals(url)))ConsentCmp.get().clear();
   ConsentCmp.init(activity,new ConsentOptions.Builder().codeConfig(key).baseUrl(url).build());
   JSONObject values=options.optJSONObject("values");if(values!=null){for(Iterator<String> keys=values.keys();keys.hasNext();){String k=keys.next();Object v=values.get(k);if(!(v instanceof String))throw new IllegalArgumentException("values must contain strings");source.put(k,(String)v);}}
  }catch(Exception e){callback.onResult(error("invalid_arguments",e.getMessage()));return;}
  active=true;AtomicBoolean finished=new AtomicBoolean();
  Dialog dialog=new Dialog(new ContextThemeWrapper(activity,com.google.android.material.R.style.Theme_MaterialComponents_Light_NoActionBar));
  Application.ActivityLifecycleCallbacks lifecycle=new Application.ActivityLifecycleCallbacks(){
   @Override public void onActivityDestroyed(Activity destroyed){if(destroyed==activity)dialog.dismiss();}
   @Override public void onActivityCreated(Activity a,Bundle b){}
   @Override public void onActivityStarted(Activity a){}
   @Override public void onActivityResumed(Activity a){}
   @Override public void onActivityPaused(Activity a){}
   @Override public void onActivityStopped(Activity a){}
   @Override public void onActivitySaveInstanceState(Activity a,Bundle b){}
  };
  activity.getApplication().registerActivityLifecycleCallbacks(lifecycle);
  dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
  ScrollView scroll=new ScrollView(dialog.getContext());LinearLayout layout=new LinearLayout(dialog.getContext());layout.setOrientation(LinearLayout.VERTICAL);
  int padding=(int)(16*activity.getResources().getDisplayMetrics().density);layout.setPadding(padding,padding,padding,padding);scroll.addView(layout);dialog.setContentView(scroll);
  TextView message=new TextView(dialog.getContext());layout.addView(message);
  boolean embedded=options.optBoolean("embedded",false);
  EditText email=new EditText(dialog.getContext());email.setHint("Email");email.setText(options.optJSONObject("values")!=null?options.optJSONObject("values").optString("Email",""):"");
  EditText phone=new EditText(dialog.getContext());phone.setHint("PhoneNumber");phone.setText(options.optJSONObject("values")!=null?options.optJSONObject("values").optString("PhoneNumber",""):"");
  if(embedded){layout.addView(email);layout.addView(phone);}
  ConsentFormView form=new ConsentFormView(dialog.getContext());form.setEmbeddedInForm(true);layout.addView(form,new LinearLayout.LayoutParams(-1,-2));
  Button submit=new Button(dialog.getContext());submit.setText(embedded?"Gửi consent trước khi đăng ký":"Lưu lựa chọn");submit.setEnabled(false);layout.addView(submit);
  final boolean[] ready={false};
  form.setOnLoadListener(new ConsentFormView.OnLoadListener(){
   @Override public void onLoaded(ConsentConfig config){ready[0]=config.isActive();submit.setEnabled(ready[0]&&form.hasAllRequired());message.setText(ready[0]?"":"Collection Point không hoạt động");}
   @Override public void onLoadFailed(ConsentException e){ready[0]=false;submit.setEnabled(false);message.setText(e.getMessage());}
  });
  form.setOnStateChangeListener((s,required)->submit.setEnabled(ready[0]&&required&&!form.isSubmitting()));
  submit.setOnClickListener(v->{
   if(finished.get()||!ready[0]||form.isSubmitting())return;
   if(embedded){source.put("Email",email.getText().toString());source.put("PhoneNumber",phone.getText().toString());}
   submit.setEnabled(false);inFlight=true;
   form.submit(source,new ConsentCallback<SendConsentResult>(){
    @Override public void onSuccess(SendConsentResult sent){inFlight=false;if(!finished.compareAndSet(false,true))return;active=false;callback.onResult(success(sent));dialog.dismiss();}
    @Override public void onError(ConsentException e){inFlight=false;if(finished.get())return;message.setText(e.getMessage());submit.setEnabled(ready[0]&&form.hasAllRequired());}
   });
  });
  Button cancel=new Button(dialog.getContext());cancel.setText("Đóng");layout.addView(cancel);cancel.setOnClickListener(v->dialog.dismiss());
  dialog.setOnDismissListener(d->{activity.getApplication().unregisterActivityLifecycleCallbacks(lifecycle);if(finished.compareAndSet(false,true)){active=false;callback.onResult(error("cancelled","Consent screen closed; an in-flight server request may still complete."));}});
  dialog.show();if(dialog.getWindow()!=null)dialog.getWindow().setLayout(-1,-1);form.setAutoLoad(true);
 }
 public static boolean clear(){if(active||inFlight)return false;if(ConsentCmp.isInitialized())ConsentCmp.get().clear();return true;}
 public static boolean isGranted(String purposeKey){return ConsentCmp.isInitialized()&&ConsentCmp.get().isGranted(purposeKey);}
 public static String savedConsentId(){ConsentState s=ConsentCmp.isInitialized()?ConsentCmp.get().savedState():null;return s==null?null:s.getConsentId();}
 private static String success(SendConsentResult result){try{return new JSONObject().put("ok",result.ok).put("consentId",result.consentId).toString();}catch(JSONException e){return error("submit_failed","Invalid SDK result");}}
 public static String error(String code,String message){try{return new JSONObject().put("ok",false).put("error",new JSONObject().put("code",code).put("message",message==null?"":message)).toString();}catch(JSONException e){throw new IllegalStateException(e);}}
}
