using System;
using System.Collections.Concurrent;
using System.Runtime.InteropServices;
using UnityEngine;

namespace Fpt.Cmp {
 public sealed class CmpConsent:MonoBehaviour {
  static CmpConsent instance;
  static readonly ConcurrentQueue<string> Results=new ConcurrentQueue<string>();
  Action<string> pending;
  void Awake(){if(instance!=null&&instance!=this){Destroy(gameObject);return;}instance=this;DontDestroyOnLoad(gameObject);}
  void Update(){while(Results.TryDequeue(out var json)){var callback=pending;pending=null;callback?.Invoke(json);}}
  public void Present(string optionsJson,Action<string> completed){
   if(pending!=null){completed("{\"ok\":false,\"error\":{\"code\":\"busy\"}}");return;}
   pending=completed;
   try {
#if UNITY_ANDROID && !UNITY_EDITOR
    using(var player=new AndroidJavaClass("com.unity3d.player.UnityPlayer"))
    using(var activity=player.GetStatic<AndroidJavaObject>("currentActivity"))
    using(var bridge=new AndroidJavaClass("vn.fpt.cmp.bridge.CmpBridge"))bridge.CallStatic("present",activity,optionsJson,new ResultProxy());
#elif UNITY_IOS && !UNITY_EDITOR
    CMPConsentPresent(optionsJson,NativeCallback);
#else
    Results.Enqueue("{\"ok\":false,\"error\":{\"code\":\"unsupported_platform\"}}");
#endif
   }catch(Exception e){pending=null;completed(JsonUtility.ToJson(new ErrorResult{error=new Error{code="bridge_failed",message=e.Message}}));}
  }
  [Serializable]class ErrorResult{public bool ok=false;public Error error;}
  [Serializable]class Error{public string code;public string message;}
#if UNITY_ANDROID && !UNITY_EDITOR
  sealed class ResultProxy:AndroidJavaProxy {
   public ResultProxy():base("vn.fpt.cmp.bridge.CmpBridge$Callback"){}
   public void onResult(string json){Results.Enqueue(json);}
  }
#endif
#if UNITY_IOS && !UNITY_EDITOR
  delegate void NativeResult(string json);
  static readonly NativeResult NativeCallback=OnNativeResult;
  [DllImport("__Internal")]static extern void CMPConsentPresent(string options,NativeResult callback);
  [AOT.MonoPInvokeCallback(typeof(NativeResult))]static void OnNativeResult(string json){Results.Enqueue(json);}
#endif
 }
}
