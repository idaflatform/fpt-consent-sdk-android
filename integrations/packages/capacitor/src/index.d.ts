export interface ConsentOptions {codeConfig:string;baseUrl?:string;values?:Record<string,string>;embedded?:boolean}
export function presentConsent(options:ConsentOptions):Promise<{ok:true;consentId:string}>;
export function clearConsent():Promise<{cleared:boolean}>;
