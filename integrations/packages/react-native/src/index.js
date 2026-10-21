import {NativeModules,Platform} from 'react-native';
const native=()=>{
 if(!['android','ios'].includes(Platform.OS))throw new Error('CMP requires Android or iOS.');
 const module=NativeModules.CmpConsent;
 if(!module)throw new Error('CmpConsent native module is not linked. Rebuild the native app; Expo Go is not supported.');
 return module;
};
export async function presentConsent(options){
 if(!options?.codeConfig?.trim())throw new Error('codeConfig is required');
 const result=JSON.parse(await native().present(JSON.stringify(options)));
 if(!result.ok){const error=new Error(result.error?.message||'Consent failed');error.code=result.error?.code||'submit_failed';throw error;}
 if(!result.consentId)throw new Error('Consent result did not contain consentId');
 return result;
}
export const clearConsent=()=>native().clear();
export const isGranted=purposeKey=>native().isGranted(purposeKey);
export const savedConsentId=()=>native().savedConsentId();
