using System;
using Foundation;
using ObjCRuntime;
using UIKit;
namespace Fpt.Cmp.Native;
[BaseType(typeof(NSObject))]
interface CMPMobileBridge {
 [Static,Export("presentFrom:optionsJSON:completion:")]
 void Present(UIViewController parent,string optionsJson,Action<string> completion);
 [Static,Export("clear")]
 bool Clear();
 [Static,Export("isGranted:")]
 bool IsGranted(string key);
 [Static,Export("savedConsentId")]
 [return:NullAllowed]
 string SavedConsentId();
}
