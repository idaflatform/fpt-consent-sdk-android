import {registerPlugin} from '@capacitor/core';
const plugin=registerPlugin('CmpConsent');
export async function presentConsent(options){
 const {json}=await plugin.present({options:JSON.stringify(options)});
 const result=JSON.parse(json);
 if(!result.ok){const error=new Error(result.error?.message||'Consent failed');error.code=result.error?.code;throw error;}
 if(!result.consentId)throw new Error('Missing consentId');
 return result;
}
export const clearConsent=()=>plugin.clear();
