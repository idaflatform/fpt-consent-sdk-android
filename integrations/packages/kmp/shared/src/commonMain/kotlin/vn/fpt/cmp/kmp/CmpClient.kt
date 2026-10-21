package vn.fpt.cmp.kmp
/** The platform supplies native presentation; core consent rules remain in the native SDK. */
interface NativeConsentPresenter {fun present(optionsJson:String,onResult:(String)->Unit)}
class CmpClient(private val presenter:NativeConsentPresenter){
 fun open(optionsJson:String,onResult:(String)->Unit){presenter.present(optionsJson,onResult)}
}
