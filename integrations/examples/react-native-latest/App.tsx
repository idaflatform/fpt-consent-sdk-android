import React,{useState} from 'react';
import {Button,SafeAreaView,ScrollView,Text,TextInput} from 'react-native';
import {presentConsent,clearConsent} from '@fpt-cmp/react-native-consent';
export default function App(){
 const [key,setKey]=useState(''),[url,setUrl]=useState('https://cmp.biznext.vn'),[email,setEmail]=useState('demo@example.invalid'),[message,setMessage]=useState(''),[busy,setBusy]=useState(false);
 const open=async(embedded:boolean)=>{setBusy(true);try{const result=await presentConsent({codeConfig:key,baseUrl:url,embedded,values:{Email:email}});setMessage(`Đã gửi consentId=${result.consentId}`);}catch(e){setMessage(String(e));}finally{setBusy(false);}};
 return <SafeAreaView><ScrollView contentContainerStyle={{padding:16,gap:12}}>
  <Text>CMP React Native</Text><TextInput placeholder="Integration key" value={key} onChangeText={setKey}/><TextInput value={url} onChangeText={setUrl}/><TextInput value={email} onChangeText={setEmail}/>
  <Button title="Consent độc lập" disabled={busy||!key} onPress={()=>open(false)}/><Button title="Đăng ký trong màn native" disabled={busy||!key} onPress={()=>open(true)}/>
  <Button title="Đăng xuất" disabled={busy} onPress={async()=>setMessage(await clearConsent()?'Đã xóa':'Đang có màn consent mở')}/><Text>{message}</Text>
 </ScrollView></SafeAreaView>;
}
