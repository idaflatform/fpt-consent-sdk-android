#import <UIKit/UIKit.h>
#import <CMPMobileBridge/CMPMobileBridge-Swift.h>
extern UIViewController* UnityGetGLViewController(void);
extern "C" void CMPConsentPresent(const char *options,void (*callback)(const char *)) {
 NSString *json=options ? [NSString stringWithUTF8String:options] : @"{}";
 dispatch_async(dispatch_get_main_queue(),^{
  [CMPMobileBridge presentFrom:UnityGetGLViewController() optionsJSON:json completion:^(NSString *result){callback(result.UTF8String);}];
 });
}
