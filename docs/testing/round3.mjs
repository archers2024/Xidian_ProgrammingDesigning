import {base as testBase,testPassword,testUsername} from './legacy-test-env.mjs';
import fs from 'node:fs';
import {spawn,spawnSync} from 'node:child_process';
const base=testBase,tag='ROUND3_'+Date.now(),results=[],ids=[];
const env={...process.env,MYSQL_PWD:process.env.TEST_DB_PASSWORD};
const mysql=process.env.MYSQL_BIN||'mysql',args=['--host=127.0.0.1','--user='+process.env.TEST_DB_USER,'--database='+process.env.TEST_SCHEMA,'--batch','--skip-column-names'];
function sql(text){const r=spawnSync(mysql,[...args,'-e',text],{env,encoding:'utf8'});if(r.status!==0)throw Error(r.stderr);return r.stdout.trim();}
let token='';
async function req(path,method='GET',body){const r=await fetch(base+path,{method,headers:{Authorization:'Bearer '+token,...(body!==undefined?{'Content-Type':'application/json'}:{})},body:body===undefined?undefined:JSON.stringify(body)});let json=await r.json();return {http:r.status,...json};}
const check=(name,pass,evidence)=>results.push({name,pass:!!pass,evidence});
const time=h=>new Date(Date.now()+(h+8)*3600000).toISOString().slice(0,19);
async function duringLock(id,action){
 let ready,failed;const started=new Promise((r,j)=>{ready=r;failed=j});
 const child=spawn(mysql,[...args,'--unbuffered','-e',`START TRANSACTION; UPDATE t_activity SET signed_count=1 WHERE activity_id=${id}; SELECT 'LOCKED'; DO SLEEP(1); COMMIT;`],{env});
 let stderr='';child.stdout.on('data',b=>{if(b.toString().includes('LOCKED'))ready()});child.stderr.on('data',b=>stderr+=b);child.on('error',failed);
 const done=new Promise((r,j)=>child.on('close',code=>{if(code===0)r();else{failed(Error(stderr));j(Error(stderr))}}));
 await started;const response=await action();await done;return response;
}
try{
 token=(await req('/auth/login','POST',{username:testUsername,password:testPassword})).data.token;
 const body={title:tag,typeCode:'LECTURE',location:'回归测试地点',quota:5,signupDeadline:time(1),startTime:time(2),endTime:time(3)};
 let r=await req('/admin/activity','POST',body);if(r.code!==0)throw Error('create fixture: '+r.message);const id=r.data.activityId;ids.push(id);
 await req('/admin/activity/'+id+'/publish','POST');
 sql(`UPDATE t_activity SET signup_deadline=DATE_SUB(NOW(),INTERVAL 1 HOUR),status='OPEN' WHERE activity_id=${id}`);
 await duringLock(id,()=>req('/admin/activity/'+id));
 check('详情读取不能覆盖并发报名计数',sql(`SELECT signed_count FROM t_activity WHERE activity_id=${id}`)==='1');
 sql(`UPDATE t_activity SET signed_count=0,status='OPEN' WHERE activity_id=${id}`);
 await duringLock(id,()=>req('/admin/activity/list'));
 check('状态批量刷新不能覆盖报名计数',sql(`SELECT signed_count FROM t_activity WHERE activity_id=${id}`)==='1');
 sql(`UPDATE t_activity SET signed_count=0,status='CLOSED' WHERE activity_id=${id}`);
 r=await duringLock(id,()=>req('/admin/activity/'+id,'PUT',body));
 check('编辑等待行锁并保留最新计数',r.code===0&&sql(`SELECT signed_count FROM t_activity WHERE activity_id=${id}`)==='1',{code:r.code});
 r=await req('/admin/activity/'+id,'PUT',{...body,quota:1});
 let promo=await req('/promo/'+(await req('/admin/activity/'+id)).data.activity.signupGuid);
 check('满额宣传页关闭报名',r.code===0&&promo.data.signupOpen===false,{signupOpen:promo.data.signupOpen});
 for(const attachments of [[null],[{fileName:'x'.repeat(300),url:'/api/v1/files/test.pdf'}],[{fileName:'x.pdf',url:'javascript:alert(1)'}]]){
  r=await req('/admin/activity','POST',{...body,attachments});if(r.code===0)ids.push(r.data.activityId);
  check('非法附件返回参数错误 '+JSON.stringify(attachments).slice(0,45),r.code===40001,{code:r.code});
 }
 for(const patch of [{liveUrl:'javascript:alert(1)'},{coverUrl:'data:text/html,probe'},{title:'oversized',location:'x'.repeat(201)}]){
  r=await req('/admin/activity','POST',{...body,...patch});if(r.code===0)ids.push(r.data.activityId);
  check('活动链接与长度边界 '+Object.keys(patch).join(','),r.code===40001,{code:r.code});
 }
 r=await req('/super/admin/accounts','POST',{username:tag,realName:'测试',dept:'x'.repeat(101),passwordHash:'Test12345'});
 check('超长部门返回参数错误',r.code===40001,{code:r.code});
 const fd=new FormData();fd.append('file',new Blob(['PKinvalid-office']),'invalid.docx');
 const upload=await fetch(base+'/file/upload',{method:'POST',headers:{Authorization:'Bearer '+token},body:fd});r=await upload.json();check('伪装Office文件被拒绝',r.code===40001,{code:r.code});
}catch(e){check('完整执行第三轮',false,{error:e.message});}
finally{
 for(const id of ids)await req('/admin/activity/'+id,'DELETE');
 sql(`UPDATE t_admin SET status=0,token_version=token_version+1 WHERE username='${tag}'`);
 const output={tag,time:new Date().toISOString(),summary:{total:results.length,passed:results.filter(x=>x.pass).length,failed:results.filter(x=>!x.pass).length},results};
 fs.writeFileSync('docs/testing/round3-results.json',JSON.stringify(output,null,2));console.log(JSON.stringify(output,null,2));if(output.summary.failed)process.exitCode=1;
}
