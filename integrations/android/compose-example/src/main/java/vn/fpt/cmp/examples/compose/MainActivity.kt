package vn.fpt.cmp.examples.compose

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import vn.fpt.fis.cmp.consent.*
import vn.fpt.fis.cmp.consent.model.*
import vn.fpt.fis.cmp.consent.ui.ConsentFormView

class MainActivity : AppCompatActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  setContent {
   var email by remember { mutableStateOf("demo@example.invalid") }
   var result by remember { mutableStateOf("") }
   var ready by remember { mutableStateOf(false) }
   var busy by remember { mutableStateOf(false) }
   var required by remember { mutableStateOf(false) }
   var form by remember { mutableStateOf<ConsentFormView?>(null) }
   MaterialTheme { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
    Text("Đăng ký mẫu với Compose",style=MaterialTheme.typography.headlineSmall)
    if(!ConsentCmp.isInitialized()) Text("Chưa cấu hình CMP. Build với -PcmpCodeConfig và -PcmpBaseUrl.")
    else {
     OutlinedTextField(email,{email=it},label={Text("Email")},modifier=Modifier.fillMaxWidth())
     AndroidView(factory={context->ConsentFormView(context).also { view->
      form=view;view.setEmbeddedInForm(true)
      view.setOnLoadListener(object:ConsentFormView.OnLoadListener {
       override fun onLoaded(config:ConsentConfig){ready=config.isActive();required=view.hasAllRequired();result=if(ready)"Đã tải cấu hình" else "Collection Point không hoạt động"}
       override fun onLoadFailed(error:ConsentException){ready=false;result=error.message.orEmpty()}
      })
      view.setOnStateChangeListener { _,granted->required=granted }
      view.setAutoLoad(true)
     }},modifier=Modifier.fillMaxWidth())
     Button(enabled=ready&&required&&!busy,onClick={
      val view=form ?: return@Button
      if(view.isSubmitting)return@Button
      busy=true
      view.submit(MapValueSource().put("Email",email),object:ConsentCallback<SendConsentResult>{
       override fun onSuccess(sent:SendConsentResult){busy=false;result="Đã gửi consentId=${sent.consentId}"}
       override fun onError(error:ConsentException){busy=false;result=error.message.orEmpty()}
      })
     }){Text("Gửi consent trước khi đăng ký")}
     Button(enabled=!busy,onClick={
      ConsentCmp.get().setValueSource(MapValueSource().put("Email",email))
      ConsentCmp.get().show(supportFragmentManager,object:ConsentListener{
       override fun onCompleted(state:ConsentState,sent:SendConsentResult?){result="Đã lưu consentId=${state.consentId}"}
       override fun onDismissed(){result="Đã đóng màn hình"}
       override fun onError(error:ConsentException){result=error.message.orEmpty()}
      })
     }){Text("Mở consent độc lập")}
     Button(enabled=!busy,onClick={ConsentCmp.get().clear();ready=false;form?.reload();result="Đã xóa lựa chọn"}){Text("Đăng xuất")}
    }
    Text(result)
   }}
  }
 }
}
