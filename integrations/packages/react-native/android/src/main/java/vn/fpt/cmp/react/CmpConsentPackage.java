package vn.fpt.cmp.react;
import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.*;
import com.facebook.react.uimanager.ViewManager;
import java.util.*;
public final class CmpConsentPackage implements ReactPackage {
 public List<NativeModule> createNativeModules(ReactApplicationContext context){return Collections.singletonList(new CmpConsentModule(context));}
 public List<ViewManager> createViewManagers(ReactApplicationContext context){return Collections.emptyList();}
}
