export interface ConsentOptions {codeConfig:string;baseUrl?:string;values?:Record<string,string>;embedded?:boolean}
export interface ConsentResult {ok:true;consentId:string}
export function presentConsent(options:ConsentOptions):Promise<ConsentResult>;
export function clearConsent():Promise<boolean>;
export function isGranted(purposeKey:string):Promise<boolean>;
export function savedConsentId():Promise<string|null>;
