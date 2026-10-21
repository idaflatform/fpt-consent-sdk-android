namespace CmpExample;
public static class ConsentService {
 public static async Task<string> Present(string optionsJson){
  var completion=new TaskCompletionSource<string>(TaskCreationOptions.RunContinuationsAsynchronously);
  await MainThread.InvokeOnMainThreadAsync(()=>{
#if ANDROID
   var activity=Platform.CurrentActivity??throw new InvalidOperationException("Activity not ready");
   Vn.Fpt.Cmp.Bridge.CmpBridge.Present(activity,optionsJson,new Callback(completion));
#elif IOS
   var parent=Platform.GetCurrentUIViewController()??throw new InvalidOperationException("Presenter not ready");
   Fpt.Cmp.Native.CMPMobileBridge.Present(parent,optionsJson,json=>completion.TrySetResult(json));
#else
   throw new PlatformNotSupportedException();
#endif
  });
  return await completion.Task;
 }
#if ANDROID
 sealed class Callback:Java.Lang.Object,Vn.Fpt.Cmp.Bridge.CmpBridge.ICallback {
  readonly TaskCompletionSource<string> completion;
  public Callback(TaskCompletionSource<string> completion){this.completion=completion;}
  public void OnResult(string? json){completion.TrySetResult(json??"{}");}
 }
#endif
}
