package vn.fpt.cmp.cmp_consent

import android.content.Context
import android.view.View
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView
import vn.fpt.fis.cmp.consent.*
import vn.fpt.fis.cmp.consent.model.*
import vn.fpt.fis.cmp.consent.ui.ConsentFormView

class ConsentFormPlatformView(context:Context,id:Int,messenger:BinaryMessenger,args:Map<*,*>,private val removed:(ConsentFormPlatformView)->Unit):PlatformView {
 private val channel=MethodChannel(messenger,"vn.fpt.cmp/form/$id")
 private val container=FrameLayout(context)
 private val form=ConsentFormView(context)
 private var status:Map<String,Any?> = mapOf("state" to "loading")
 private var values=readValues(args["values"])
 private var disposed=false
 private var scheduled=false
 private var lastHeight=0
 private var pending:MethodChannel.Result?=null
 private val density=context.resources.displayMetrics.density
 val isSubmitting:Boolean get()=form.isSubmitting
 private val layout=ViewTreeObserver.OnGlobalLayoutListener { scheduleHeight() }
 init {
  form.setEmbeddedInForm(args["embedded"]!=false)
  form.setOnLoadListener(object:ConsentFormView.OnLoadListener{
   override fun onLoaded(config:ConsentConfig){update(mapOf("state" to if(config.isActive())"ready" else "inactive","allRequiredGranted" to form.hasAllRequired()));scheduleHeight()}
   override fun onLoadFailed(e:ConsentException){update(mapOf("state" to "failed","code" to if(ConsentCmp.isInitialized())"load_failed" else "not_configured","message" to e.message));scheduleHeight()}
  })
  form.setOnStateChangeListener { _,required->update(status+mapOf("allRequiredGranted" to required));scheduleHeight() }
  form.setOnSubmitResultListener { _,sent->if(!disposed)channel.invokeMethod("onSubmitted",mapOf("ok" to sent.ok,"consentId" to sent.consentId)) }
  if(ConsentCmp.isInitialized())ConsentCmp.get().setValueSource { field->values.entries.firstOrNull { it.key.equals("${field.name}|${field.dataType}",true) }?.value ?: values.entries.firstOrNull { it.key.equals(field.name,true) }?.value }
  container.addView(form,FrameLayout.LayoutParams(-1,-2));container.viewTreeObserver.addOnGlobalLayoutListener(layout)
  channel.setMethodCallHandler {call,result->
   if(disposed && call.method!="dispose"){result.error("disposed","Form disposed.",null);return@setMethodCallHandler}
   when(call.method){
    "status"->{measure();result.success(status+mapOf("height" to lastHeight/density))}
    "values"->{values=readValues(call.argument<Any>("values"));result.success(null)}
    "reload"->{if(isSubmitting)result.error("busy","Submission in progress.",null)else{reload();result.success(null)}}
    "submit"->{
     if(isSubmitting||pending!=null){result.error("busy","Submission in progress.",null);return@setMethodCallHandler}
     if(status["state"]!="ready"){result.error("not_ready","Form is not ready or inactive.",null);return@setMethodCallHandler}
     pending=result;val source=MapValueSource();readValues(call.argument<Any>("values")).forEach{(k,v)->source.put(k,v)}
     form.submit(source,object:ConsentCallback<SendConsentResult>{
      override fun onSuccess(sent:SendConsentResult){val reply=pending;pending=null;reply?.success(mapOf("ok" to sent.ok,"consentId" to sent.consentId))}
      override fun onError(error:ConsentException){val reply=pending;pending=null;reply?.error("submit_failed",error.message,mapOf("httpStatus" to error.httpStatus))}
     })
    }
    "dispose"->{dispose();result.success(null)}
    else->result.notImplemented()
   }
  }
  form.setAutoLoad(true)
 }
 fun reload(){update(mapOf("state" to "loading"));form.reload()}
 private fun update(next:Map<String,Any?>){status=next;if(!disposed)channel.invokeMethod("onStatus",status)}
 private fun scheduleHeight(){if(disposed||scheduled)return;scheduled=true;container.post{scheduled=false;if(!disposed)measure()}}
 private fun measure(){if(container.width<=0)return;form.measure(View.MeasureSpec.makeMeasureSpec(container.width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));val h=form.measuredHeight;if(h>0&&h!=lastHeight){lastHeight=h;channel.invokeMethod("onHeight",h/density)}}
 override fun getView():View=container
 override fun dispose(){if(disposed)return;disposed=true;container.viewTreeObserver.removeOnGlobalLayoutListener(layout);pending?.error("disposed","Form closed before submission completed.",null);pending=null;channel.setMethodCallHandler(null);removed(this)}
 private fun readValues(raw:Any?):Map<String,String> = (raw as? Map<*,*>)?.entries?.mapNotNull { (k,v)->if(k is String&&v is String)k to v else null }?.toMap().orEmpty()
}
