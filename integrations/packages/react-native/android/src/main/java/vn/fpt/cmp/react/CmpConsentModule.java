package vn.fpt.cmp.react;
import com.facebook.react.bridge.*;
import vn.fpt.cmp.bridge.CmpBridge;
public final class CmpConsentModule extends ReactContextBaseJavaModule {
 CmpConsentModule(ReactApplicationContext context){super(context);}
 @Override public String getName(){return "CmpConsent";}
 @ReactMethod public void present(String options,Promise promise){CmpBridge.present(getCurrentActivity(),options,promise::resolve);}
 @ReactMethod public void clear(Promise promise){getReactApplicationContext().runOnUiQueueThread(()->promise.resolve(CmpBridge.clear()));}
 @ReactMethod public void isGranted(String key,Promise promise){getReactApplicationContext().runOnUiQueueThread(()->promise.resolve(CmpBridge.isGranted(key)));}
 @ReactMethod public void savedConsentId(Promise promise){getReactApplicationContext().runOnUiQueueThread(()->promise.resolve(CmpBridge.savedConsentId()));}
}
