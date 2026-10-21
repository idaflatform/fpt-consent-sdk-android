package vn.fpt.cmp.capacitor;
import com.getcapacitor.*;
import com.getcapacitor.annotation.CapacitorPlugin;
import vn.fpt.cmp.bridge.CmpBridge;
@CapacitorPlugin(name="CmpConsent")
public final class CmpConsentPlugin extends Plugin {
 @PluginMethod public void present(PluginCall call){CmpBridge.present(getActivity(),call.getString("options","{}"),json->{JSObject data=new JSObject();data.put("json",json);call.resolve(data);});}
 @PluginMethod public void clear(PluginCall call){getActivity().runOnUiThread(()->{JSObject data=new JSObject();data.put("cleared",CmpBridge.clear());call.resolve(data);});}
}
