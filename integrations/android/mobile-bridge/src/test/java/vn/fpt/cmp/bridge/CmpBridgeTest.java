package vn.fpt.cmp.bridge;
import android.app.Activity;
import android.app.Dialog;
import java.util.*;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;
import java.util.concurrent.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,manifest=Config.NONE)
public class CmpBridgeTest {
 @Test public void realSdkSubmitsMappedValuesAndReturnsConsentId()throws Exception {
  LoopbackServer server=new LoopbackServer();AtomicReference<String> submitted=server.submitted;
  Activity activity=Robolectric.buildActivity(Activity.class).setup().get();
  try {
   List<String> replies=new ArrayList<>();
   CmpBridge.present(activity,new JSONObject().put("codeConfig","demo").put("baseUrl","http://127.0.0.1:"+server.port()).put("values",new JSONObject().put("Email","demo@example.invalid")).toString(),replies::add);
   Dialog dialog=ShadowDialog.getLatestDialog();Button submit=findButton(dialog.getWindow().getDecorView(),"Lưu lựa chọn");assertNotNull(submit);
   for(int wait=0;wait<500&&!submit.isEnabled();wait++){Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();Thread.sleep(20);}
   assertTrue("SDK configuration must load",submit.isEnabled());submit.performClick();
   for(int wait=0;wait<500&&replies.isEmpty();wait++){Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();Thread.sleep(20);}
   assertEquals(1,replies.size());JSONObject response=new JSONObject(replies.get(0));assertTrue(response.getBoolean("ok"));
   JSONObject body=new JSONObject(submitted.get());assertEquals(body.getString("consentId"),response.getString("consentId"));
   JSONObject purpose=body.getJSONObject("values").getJSONObject("registration");assertTrue(purpose.getBoolean("isAccept"));
   assertEquals("demo@example.invalid",purpose.getJSONObject("email").getString("value"));
  }finally {Dialog dialog=ShadowDialog.getLatestDialog();if(dialog!=null)dialog.dismiss();server.close();activity.finish();}
 }
 private static final class LoopbackServer implements AutoCloseable {
  final ServerSocket socket;final ExecutorService worker=Executors.newSingleThreadExecutor();final AtomicReference<String> submitted=new AtomicReference<>();
  LoopbackServer()throws IOException {socket=new ServerSocket(0,10,java.net.InetAddress.getByName("127.0.0.1"));worker.execute(()->{while(!socket.isClosed()){try(Socket client=socket.accept()){serve(client);}catch(Exception e){if(!socket.isClosed())throw new RuntimeException(e);}}});}
  int port(){return socket.getLocalPort();}
  void serve(Socket client)throws Exception {
   client.setSoTimeout(10000);InputStream input=client.getInputStream();ByteArrayOutputStream header=new ByteArrayOutputStream();String text="";
   while(!text.endsWith("\r\n\r\n")){int b=input.read();if(b<0)throw new EOFException();header.write(b);text=header.toString("US-ASCII");if(header.size()>16384)throw new IOException("Header too large");}
   int size=0;for(String line:text.split("\r\n"))if(line.toLowerCase(Locale.ROOT).startsWith("content-length:"))size=Integer.parseInt(line.substring(line.indexOf(':')+1).trim());
   byte[] body=new byte[size];int offset=0;while(offset<size){int n=input.read(body,offset,size-offset);if(n<0)throw new EOFException();offset+=n;}
   String response;
   if(text.startsWith("POST ")){String json=new String(body,StandardCharsets.UTF_8);submitted.set(json);response=new JSONObject().put("data",new JSONObject().put("ok",true).put("consentId",new JSONObject(json).getString("consentId"))).toString();}
   else response="{\"data\":{\"code\":\"demo\",\"status\":\"active\",\"config\":{\"title\":\"Demo\",\"items\":[{\"id\":\"registration\",\"code\":\"registration\",\"label\":\"Register\",\"required\":true,\"dataFields\":[{\"id\":\"email\",\"name\":\"Email\",\"dataType\":\"EMAIL\",\"required\":true,\"sharedWithSystem\":true}]}]}}}";
   byte[] data=response.getBytes(StandardCharsets.UTF_8);OutputStream output=client.getOutputStream();output.write(("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: "+data.length+"\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));output.write(data);output.flush();
  }
  public void close()throws IOException {socket.close();worker.shutdownNow();}
 }
 private static Button findButton(View view,String text){if(view instanceof Button&&text.contentEquals(((Button)view).getText()))return (Button)view;if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){Button found=findButton(group.getChildAt(i),text);if(found!=null)return found;}}return null;}
 @Test public void missingActivityReturnsStructuredError()throws Exception {
  List<String> replies=new ArrayList<>();CmpBridge.present(null,"{}",replies::add);
  assertEquals(1,replies.size());assertEquals("not_ready",new JSONObject(replies.get(0)).getJSONObject("error").getString("code"));
 }
 @Test public void malformedConfigurationDoesNotOpenDialogOrLeaveBridgeBusy()throws Exception {
  Activity activity=Robolectric.buildActivity(Activity.class).setup().get();
  List<String> replies=new ArrayList<>();CmpBridge.present(activity,"{\"codeConfig\":\"demo\",\"baseUrl\":\"https://user:secret@example.invalid\"}",replies::add);
  assertEquals("invalid_arguments",new JSONObject(replies.get(0)).getJSONObject("error").getString("code"));
  assertTrue(CmpBridge.clear());activity.finish();
 }
 @Test public void closingDialogCompletesExactlyOnceAndSecondScreenReturnsBusy()throws Exception {
  Activity activity=Robolectric.buildActivity(Activity.class).setup().get();
  String options="{\"codeConfig\":\"demo\",\"baseUrl\":\"http://127.0.0.1:1\"}";
  List<String> first=new ArrayList<>(),second=new ArrayList<>();
  CmpBridge.present(activity,options,first::add);
  CmpBridge.present(activity,options,second::add);
  assertEquals("busy",new JSONObject(second.get(0)).getJSONObject("error").getString("code"));
  Dialog dialog=ShadowDialog.getLatestDialog();assertNotNull(dialog);dialog.dismiss();dialog.dismiss();
  Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
  assertEquals(1,first.size());assertEquals("cancelled",new JSONObject(first.get(0)).getJSONObject("error").getString("code"));
  assertTrue(CmpBridge.clear());activity.finish();
 }
}
