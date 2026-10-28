import {test} from 'node:test';
import assert from 'node:assert/strict';
import {createFixtureServer} from './fixture-server.mjs';
test('fixture validates consent and stores evidence without external requests',async()=>{
 const {server,records}=createFixtureServer();await new Promise(r=>server.listen(0,'127.0.0.1',r));
 const base=`http://127.0.0.1:${server.address().port}`;
 try{
  const c=await(await fetch(`${base}/api/v1/cmp/consent/config?code_config=cp_demo`)).json();assert.equal(c.data.config.items.length,2);
  assert.equal(c.data.appearance.revision,'fixture-purple-1');
  assert.equal(c.data.appearance.theme.colors.primary,'#673AB7');
  const post=body=>fetch(`${base}/api/v1/cmp/consent/sendData`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(body)});
  assert.equal((await post({values:{}})).status,400);
  const body={consentId:'demo-consent',visitorId:'demo-visitor',code:'cp_demo',values:{registration:{isAccept:true,email:{isAccept:true,value:'demo@example.invalid'}}}};
  assert.equal((await post(body)).status,200);assert.equal(records.length,1);assert.deepEqual(records[0],body);
  assert.equal((await post({...body,code:'fail-submit'})).status,503);assert.equal(records.length,1);
  assert.equal((await fetch(`${base}/api/v1/cmp/consent/config?code_config=fail-load`)).status,503);
 }finally{await new Promise(r=>server.close(r));}
});
