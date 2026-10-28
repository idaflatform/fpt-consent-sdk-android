package vn.fpt.cmp.cmp_consent

import android.content.Context
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory
import vn.fpt.fis.cmp.consent.ConsentCmp
import vn.fpt.fis.cmp.consent.ConsentOptions
import java.net.URI

class MobileConsentPlugin: FlutterPlugin, MethodChannel.MethodCallHandler {
 private lateinit var channel:MethodChannel
 private lateinit var context:Context
 private val views=mutableSetOf<ConsentFormPlatformView>()
 private val retired=mutableSetOf<ConsentFormPlatformView>()
 override fun onAttachedToEngine(binding:FlutterPlugin.FlutterPluginBinding){
  context=binding.applicationContext;channel=MethodChannel(binding.binaryMessenger,"vn.fpt.cmp/control");channel.setMethodCallHandler(this)
  binding.platformViewRegistry.registerViewFactory("vn.fpt.cmp/form",object:PlatformViewFactory(StandardMessageCodec.INSTANCE){
   override fun create(context:Context,id:Int,args:Any?):PlatformView {
    val parameters=args as? Map<*,*> ?: emptyMap<Any,Any>()
    return ConsentFormPlatformView(context,id,binding.binaryMessenger,parameters){views.remove(it);if(it.isSubmitting)retired.add(it)}.also{views.add(it)}
   }
  })
 }
 override fun onMethodCall(call:MethodCall,result:MethodChannel.Result){
  retired.removeAll{!it.isSubmitting}
  when(call.method){
   "configure"->{
    if(views.isNotEmpty()||retired.isNotEmpty()){result.error("busy","Dispose forms and wait for pending submissions before configuring.",null);return}
    val key=call.argument<String>("codeConfig")?.trim().orEmpty()
    val url=call.argument<String>("baseUrl").orEmpty()
    val uri=runCatching{URI(url)}.getOrNull()
    if(key.isEmpty()||uri==null||uri.scheme !in listOf("https","http")||uri.host.isNullOrEmpty()||!uri.userInfo.isNullOrEmpty()||!uri.query.isNullOrEmpty()||!uri.fragment.isNullOrEmpty()||uri.path !in listOf("","/")){result.error("invalid_arguments","Expected key and HTTP(S) origin.",null);return}
    if(ConsentCmp.isInitialized() && (ConsentCmp.get().options().codeConfig!=key || ConsentCmp.get().options().baseUrl!=url))ConsentCmp.get().clear()
    ConsentCmp.init(context,ConsentOptions.Builder().baseUrl(url).codeConfig(key).build());result.success(null)
   }
   "clear"->{
    if(!ConsentCmp.isInitialized()){result.error("not_configured","Configure CMP first.",null);return}
    if(views.any{it.isSubmitting}||retired.isNotEmpty()){result.error("busy","Submission is in progress.",null);return}
    ConsentCmp.get().clear();views.forEach{it.reload()};result.success(null)
   }
   "isGranted"->result.success(ConsentCmp.isInitialized()&&ConsentCmp.get().isGranted(call.argument<String>("purposeKey").orEmpty()))
   "savedConsentId"->result.success(if(ConsentCmp.isInitialized())ConsentCmp.get().savedState()?.consentId else null)
   else->result.notImplemented()
  }
 }
 override fun onDetachedFromEngine(binding:FlutterPlugin.FlutterPluginBinding){views.toList().forEach{it.dispose()};views.clear();channel.setMethodCallHandler(null)}
}
