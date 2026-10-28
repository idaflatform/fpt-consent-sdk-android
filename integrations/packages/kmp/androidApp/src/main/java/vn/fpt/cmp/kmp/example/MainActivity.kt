package vn.fpt.cmp.kmp.example
import android.app.Activity
import android.os.Bundle
import android.widget.*
import org.json.JSONObject
import vn.fpt.cmp.kmp.*
class MainActivity:Activity(){
 override fun onCreate(state:Bundle?){super.onCreate(state)
  val client=CmpClient(AndroidConsentPresenter(this));val layout=LinearLayout(this);layout.orientation=LinearLayout.VERTICAL;layout.setPadding(24,24,24,24);setContentView(layout)
  val key=EditText(this).also{it.hint="Integration key";layout.addView(it)}
  val origin=EditText(this).also{it.setText("https://cmp.biznext.vn");layout.addView(it)}
  val result=TextView(this)
  listOf(false,true).forEach { embedded->Button(this).also {button->button.text=if(embedded)"Đăng ký native" else "Consent độc lập";layout.addView(button);button.setOnClickListener{client.open(JSONObject().put("codeConfig",key.text.toString()).put("baseUrl",origin.text.toString()).put("embedded",embedded).put("values",JSONObject().put("Email","demo@example.invalid")).toString()){result.text=it}}} }
  layout.addView(result)
 }
}
