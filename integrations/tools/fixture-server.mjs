import http from 'node:http';
import {pathToFileURL} from 'node:url';

export const config={code:'cp_demo::t_demo',name:'CMP integration fixture',status:'active',fields:[],config:{
 title:'Mục đích xử lý dữ liệu',description:'Dữ liệu giả lập phục vụ kiểm thử tích hợp SDK.',submitLabel:'Lưu lựa chọn',items:[
 {id:'registration',code:'registration',label:'Đăng ký tài khoản mẫu',description:'Kiểm tra luồng đăng ký nhúng.',required:true,dataFields:[
 {id:'email',name:'Email',dataType:'EMAIL',display:true,required:true,sharedWithSystem:true}],thirdParties:[]},
 {id:'marketing',code:'marketing',label:'Nhận thông tin giới thiệu sản phẩm',description:'Mục đích tùy chọn; người dùng có thể từ chối.',required:false,dataFields:[
 {id:'phone',name:'PhoneNumber',dataType:'PHONE',display:true,required:false,sharedWithSystem:true}],thirdParties:[]} ]}};

export function createFixtureServer(){
 const records=[];
 const server=http.createServer(async(req,res)=>{
  res.setHeader('Content-Type','application/json; charset=utf-8');
  res.setHeader('Access-Control-Allow-Origin','*');
  res.setHeader('Access-Control-Allow-Headers','Content-Type,X-Consent-Integration,Cache-Control');
  if(req.method==='OPTIONS'){res.writeHead(204).end();return;}
  const url=new URL(req.url,'http://localhost');
  const reply=(status,data)=>{res.writeHead(status);res.end(JSON.stringify(data));};
  if(req.method==='GET'&&url.pathname==='/health'){reply(200,{ok:true});return;}
  if(req.method==='GET'&&url.pathname==='/records'){reply(200,{records});return;}
  if(url.pathname.endsWith('/config')&&req.method==='GET'){
   if(url.searchParams.get('code_config')==='fail-load'){reply(503,{message:'Fixture load failed'});return;}
   reply(200,{data:{...config,status:url.searchParams.get('code_config')==='inactive'?'inactive':'active'}});return;
  }
  if(url.pathname.endsWith('/sendData')&&req.method==='POST'){
   try{
    let body='';for await(const chunk of req){body+=chunk;if(body.length>65536){reply(413,{message:'Too large'});return;}}
    const data=JSON.parse(body);
    if(data.code==='fail-submit'){reply(503,{message:'Fixture submit failed'});return;}
    if(!data.consentId||!data.visitorId||!data.values||data.values.registration?.isAccept!==true){reply(400,{message:'Required registration consent missing'});return;}
    records.push(data);reply(200,{data:{ok:true,consentId:data.consentId,code:data.code,dateCreated:data.dateCreated}});
   }catch{reply(400,{message:'Invalid JSON'});}return;
  }
  reply(404,{message:'Not found'});
 });
 return {server,records};
}
if(process.argv[1]&&import.meta.url===pathToFileURL(process.argv[1]).href){
 const {server}=createFixtureServer();server.listen(Number(process.env.PORT||8099),'127.0.0.1',()=>console.log('CMP fixture http://127.0.0.1:8099 (synthetic data only)'));
}
