var exec=require('cordova/exec');
exports.present=function(options){return new Promise(function(resolve,reject){exec(function(json){try{var result=typeof json==='string'?JSON.parse(json):json;if(result.ok&&result.consentId)resolve(result);else reject(Object.assign(new Error(result.error&&result.error.message||'Consent failed'),result.error));}catch(error){reject(error);}},reject,'CmpConsent','present',[JSON.stringify(options)]);});};
exports.clear=function(){return new Promise(function(resolve,reject){exec(resolve,reject,'CmpConsent','clear',[]);});};
