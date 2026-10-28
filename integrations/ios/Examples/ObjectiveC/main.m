#import <UIKit/UIKit.h>
#import <CMPMobileBridge/CMPMobileBridge-Swift.h>
@interface ExampleController:UIViewController
@end
@implementation ExampleController {
 UITextField *_key,*_origin;
 UILabel *_result;
}
-(void)viewDidLoad {
 [super viewDidLoad];self.view.backgroundColor=UIColor.systemBackgroundColor;
 UIStackView *stack=[UIStackView new];stack.axis=UILayoutConstraintAxisVertical;stack.spacing=16;stack.translatesAutoresizingMaskIntoConstraints=NO;[self.view addSubview:stack];
 [NSLayoutConstraint activateConstraints:@[[stack.topAnchor constraintEqualToAnchor:self.view.safeAreaLayoutGuide.topAnchor constant:24],[stack.leadingAnchor constraintEqualToAnchor:self.view.leadingAnchor constant:16],[stack.trailingAnchor constraintEqualToAnchor:self.view.trailingAnchor constant:-16]]];
 _key=[UITextField new];_key.placeholder=@"Integration key";_origin=[UITextField new];_origin.text=@"https://cmp.biznext.vn";
 [stack addArrangedSubview:_key];[stack addArrangedSubview:_origin];
 for(int i=0;i<2;i++){UIButton *button=[UIButton buttonWithType:UIButtonTypeSystem];button.tag=i;[button setTitle:i?@"Đăng ký native":@"Consent độc lập" forState:UIControlStateNormal];[button addTarget:self action:@selector(open:) forControlEvents:UIControlEventTouchUpInside];[stack addArrangedSubview:button];}
 _result=[UILabel new];_result.numberOfLines=0;[stack addArrangedSubview:_result];
}
-(void)open:(UIButton*)sender {
 NSDictionary *options=@{@"codeConfig":_key.text?:@"",@"baseUrl":_origin.text?:@"",@"embedded":@(sender.tag==1),@"values":@{@"Email":@"demo@example.invalid"}};
 NSData *data=[NSJSONSerialization dataWithJSONObject:options options:0 error:nil];NSString *json=[[NSString alloc]initWithData:data encoding:NSUTF8StringEncoding];
 __weak ExampleController *weakSelf=self;
 [CMPMobileBridge presentFrom:self optionsJSON:json completion:^(NSString *result){ExampleController *strongSelf=weakSelf;if(strongSelf)strongSelf->_result.text=result;}];
}
@end
@interface AppDelegate:UIResponder<UIApplicationDelegate>
@property(nonatomic,strong)UIWindow *window;
@end
@implementation AppDelegate
-(BOOL)application:(UIApplication*)application didFinishLaunchingWithOptions:(NSDictionary*)options {
 self.window=[[UIWindow alloc]initWithFrame:UIScreen.mainScreen.bounds];self.window.rootViewController=[ExampleController new];[self.window makeKeyAndVisible];return YES;
}
@end
int main(int argc,char *argv[]){@autoreleasepool{return UIApplicationMain(argc,argv,nil,NSStringFromClass(AppDelegate.class));}}
