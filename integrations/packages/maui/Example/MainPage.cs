using System.Text.Json;
namespace CmpExample;
public sealed class MainPage:ContentPage {
 readonly Entry key=new(){Placeholder="Integration key"},origin=new(){Text="https://cmp.biznext.vn"},email=new(){Text="demo@example.invalid"};
 readonly Label result=new();
 readonly Button standalone=new(){Text="Consent độc lập"},registration=new(){Text="Đăng ký native"};
 public MainPage(){
  Title="CMP MAUI";Content=new ScrollView{Content=new VerticalStackLayout{Padding=16,Spacing=12,Children={key,origin,email,standalone,registration,result}}};
  standalone.Clicked+=async(_,_)=>await Open(false);registration.Clicked+=async(_,_)=>await Open(true);
 }
 async Task Open(bool embedded){
  standalone.IsEnabled=registration.IsEnabled=false;
  try{var options=JsonSerializer.Serialize(new{codeConfig=key.Text,baseUrl=origin.Text,embedded,values=new Dictionary<string,string>{{"Email",email.Text??""}}});result.Text=await ConsentService.Present(options);}
  catch(Exception e){result.Text=e.Message;}
  finally{standalone.IsEnabled=registration.IsEnabled=true;}
 }
}
