package vn.fpt.cmp.kmp
import android.app.Activity
import vn.fpt.cmp.bridge.CmpBridge
class AndroidConsentPresenter(private val activity:Activity):NativeConsentPresenter {
 override fun present(optionsJson:String,onResult:(String)->Unit){CmpBridge.present(activity,optionsJson){onResult(it)}}
}
