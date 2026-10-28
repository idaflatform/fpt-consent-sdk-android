#import <Cordova/CDV.h>
#import <CMPMobileBridge/CMPMobileBridge-Swift.h>
@interface CmpConsentPlugin:CDVPlugin
@end
@implementation CmpConsentPlugin
-(void)present:(CDVInvokedUrlCommand*)command {
 dispatch_async(dispatch_get_main_queue(),^{
  [CMPMobileBridge presentFrom:self.viewController optionsJSON:command.arguments.firstObject completion:^(NSString *json){
   [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:json] callbackId:command.callbackId];
  }];
 });
}
-(void)clear:(CDVInvokedUrlCommand*)command {
 dispatch_async(dispatch_get_main_queue(),^{[self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:[CMPMobileBridge clear]] callbackId:command.callbackId];});
}
@end
