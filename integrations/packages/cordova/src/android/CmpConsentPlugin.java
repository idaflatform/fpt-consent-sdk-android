package vn.fpt.cmp.cordova;
import org.apache.cordova.*;
import org.json.*;
import vn.fpt.cmp.bridge.CmpBridge;
public final class CmpConsentPlugin extends CordovaPlugin {
 @Override public boolean execute(String action,JSONArray args,CallbackContext callback)throws JSONException{
  if(action.equals("present")){CmpBridge.present(cordova.getActivity(),args.getString(0),callback::success);return true;}
  if(action.equals("clear")){cordova.getActivity().runOnUiThread(()->callback.success(CmpBridge.clear()?1:0));return true;}
  return false;
 }
}
