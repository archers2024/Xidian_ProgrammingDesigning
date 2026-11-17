import {base,testPassword,testUsername} from './test-env.mjs';
import fs from 'node:fs';
import {spawnSync} from 'node:child_process';
import {createHmac,randomUUID} from 'node:crypto';
const tag='ENT_'+Date.now(),results=[],activities=[],accounts=[],assets=[];let root='';
function check(name,pass,evidence){results.push({name,pass:!!pass,evidence})}
async function raw(route,method='GET',body,token=root,headers={}){return fetch(base+route,{method,headers:{...(token?{Authorization:'Bearer '+token}:{}),...(body===undefined?{}:{'Content-Type':'application/json'}),...headers},body:body===undefined?undefined:JSON.stringify(body)})}
async function req(route,method='GET',body,token=root,headers={}){const r=await raw(route,method,body,token,headers);return{http:r.status,...await r.json().catch(()=>({}))}}
function sql(statement,privileged=false){const result=spawnSync(process.env.MYSQL_BIN,['--host=127.0.0.1','--user='+(privileged?'root':process.env.TEST_DB_USER),'--database='+process.env.TEST_SCHEMA,'--batch','--skip-column-names','-e',statement],{env:{...process.env,MYSQL_PWD:privileged?(process.env.TEST_ROOT_PASSWORD||''):process.env.TEST_DB_PASSWORD},encoding:'utf8'});if(result.status!==0)throw Error(result.stderr);return result.stdout.trim()}
const time=h=>new Date(Date.now()+(8+h)*3600000).toISOString().slice(0,19);
const body={title:tag,typeCode:'LECTURE',location:'企业验收场地',quota:500,signupDeadline:time(1),startTime:time(2),endTime:time(3)};
async function create(extra={}){const r=await req('/admin/activity','POST',{...body,...extra});if(r.code!==0)throw Error('fixture: '+r.message);activities.push(r.data.activityId);return r.data.activityId}
async function user(suffix,role='ADMIN',orgId=1){const r=await req('/super/admin/accounts','POST',{username:tag+suffix,realName:'验收账号',passwordHash:'Enterprise12345',roleCode:role,orgId});if(r.code!==0)throw Error(r.message);const id=Number(sql(`SELECT admin_id FROM t_admin WHERE username='${tag+suffix}'`));accounts.push(id);const token=(await req('/auth/login','POST',{username:tag+suffix,password:'Enterprise12345'},'')).data.token;return{id,token,username:tag+suffix}}
function proof(phone,extra={}){const encode=x=>Buffer.from(JSON.stringify(x)).toString('base64url');const content=encode({alg:'HS256',typ:'JWT'})+'.'+encode({iss:process.env.TEST_IDENTITY_ISSUER,aud:'campus-attendance',sub:'fixture-student',phone_number:phone,phone_number_verified:true,exp:Math.floor(Date.now()/1000)+120,...extra});return content+'.'+createHmac('sha256',process.env.TEST_IDENTITY_SECRET).update(content).digest('base64url')}
async function upload(visibility,size=32){const data=new FormData();data.append('file',new Blob(['%PDF-1.4\n'+'x'.repeat(size)],{type:'application/pdf'}),'enterprise.pdf');data.append('visibility',visibility);const r=await fetch(base+'/file/upload',{method:'POST',headers:{Authorization:'Bearer '+root},body:data});const json=await r.json();if(json.code===0)assets.push(json.data.url.split('/').at(-1));return json}
try{
 root=(await req('/auth/login','POST',{username:testUsername,password:testPassword},'')).data.token;
 const ready=await raw('/actuator/health/readiness');check('就绪探针可用',(await ready.json()).status==='UP'&&ready.status===200);check('响应带追踪ID和禁止缓存',!!ready.headers.get('x-request-id')&&ready.headers.get('cache-control')==='no-store');
 check('匿名指标接口拒绝',(await raw('/actuator/prometheus','GET',undefined,'')).status===401);
 check('超管可读指标',(await raw('/actuator/prometheus')).status===200);
 check('不存在接口正确404',(await req('/missing_enterprise_'+tag)).http===404);
 check('URL中的JWT不作为凭据',(await raw('/admin/current?token='+root,'GET',undefined,'')).status===401);
 for(const [origin,allowed] of [['http://localhost:5173',true],['https://untrusted.invalid',false]]){const r=await raw('/admin/current','OPTIONS',undefined,'',{'Origin':origin,'Access-Control-Request-Method':'GET','Access-Control-Request-Headers':'authorization'});check('预检来源策略 '+origin,allowed?r.status===200&&r.headers.get('access-control-allow-origin')===origin:r.status===403&&!r.headers.has('access-control-allow-origin'))}
 const a=await user('A'),b=await user('B');
 const beforePassword=sql(`SELECT password_hash FROM t_admin WHERE admin_id=${b.id}`);
 sql("CREATE TRIGGER injected_audit_failure BEFORE INSERT ON t_operation_log FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='injected audit failure'",true);
 let injected;
 try{injected=await req('/super/admin/accounts/'+b.id+'/password','PUT')}finally{sql('DROP TRIGGER injected_audit_failure',true)}
 check('审计故障导致账号写操作失败',injected.http===500);
 check('审计故障时密码更新被事务回滚',sql(`SELECT password_hash FROM t_admin WHERE admin_id=${b.id}`)===beforePassword);

 const login=await raw('/auth/login','POST',{username:a.username,password:'Enterprise12345'},'');const cookies=login.headers.getSetCookie();const cookie=cookies.map(value=>value.split(';')[0]).join('; '),csrf=cookies.find(value=>value.startsWith('CAMPUS_CSRF=')).split(';')[0].split('=')[1];
 check('会话Cookie设为HttpOnly',cookies.some(value=>value.startsWith('CAMPUS_SESSION=')&&/httponly/i.test(value)&&/samesite=strict/i.test(value)));
 check('Cookie会话可读取当前身份',(await req('/admin/current','GET',undefined,'',{Cookie:cookie})).code===0);
 check('缺少CSRF令牌拒绝Cookie写操作',(await raw('/admin/password','PUT',{oldPassword:'wrong',newPassword:'NewEnterprise123'},'',{Cookie:cookie})).status===403);
 check('错误授权头不能绕过CSRF',(await raw('/admin/password','PUT',{oldPassword:'wrong',newPassword:'NewEnterprise123'},'',{Cookie:cookie,Authorization:'Basic invalid'})).status===403);
 const withCsrf=await req('/admin/password','PUT',{oldPassword:'wrong',newPassword:'NewEnterprise123'},'',{Cookie:cookie,'X-CSRF-Token':csrf});check('正确CSRF到达业务校验',withCsrf.http!==403&&withCsrf.code===40001);
 const temp=await req('/super/admin/accounts','POST',{username:tag+'TEMP',realName:'随机密码验收'});const tempId=Number(sql(`SELECT admin_id FROM t_admin WHERE username='${tag}TEMP'`));accounts.push(tempId);
 check('临时密码随机且不使用固定值',temp.data.temporaryPassword.length>=20&&!['Admin12345','admin123'].includes(temp.data.temporaryPassword));
 const reset1=(await req('/super/admin/accounts/'+tempId+'/password','PUT')).data.temporaryPassword,reset2=(await req('/super/admin/accounts/'+tempId+'/password','PUT')).data.temporaryPassword;
 check('连续重置返回不同密码',reset1!==reset2);check('旧临时密码失效',(await req('/auth/login','POST',{username:tag+'TEMP',password:reset1},'')).code!==0);
 const tempToken=(await req('/auth/login','POST',{username:tag+'TEMP',password:reset2},'')).data.token;check('随机临时密码首次强制改密',(await req('/admin/activity/list','GET',undefined,tempToken)).http===403);
 const oldHash=sql(`SELECT password_hash FROM t_admin WHERE admin_id=${a.id}`);
 try{sql("CREATE TRIGGER enterprise_audit_failure BEFORE INSERT ON t_operation_log FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='injected audit failure'",true);check('审计失败返回系统错误',(await req('/super/admin/accounts/'+a.id+'/password','PUT')).http===500);check('审计失败时密码更新回滚',sql(`SELECT password_hash FROM t_admin WHERE admin_id=${a.id}`)===oldHash)}finally{sql('DROP TRIGGER IF EXISTS enterprise_audit_failure',true)}
 const id=await create(),path='/admin/activity/'+id;const original=(await req(path)).data.activity;
 check('缺少编辑版本被拒',(await req(path,'PUT',{...body,title:tag+'missing'})).http===409);
 check('匹配版本保存成功',(await req(path,'PUT',{...body,title:tag+'新版',version:original.version})).code===0);
 check('过期版本不会覆盖新版',(await req(path,'PUT',{...body,title:tag+'旧版',version:original.version})).http===409);
 check('编辑冲突后内容保持新版',(await req(path)).data.activity.title===tag+'新版');
 const key=randomUUID();const duplicate=await Promise.all([req('/admin/activity','POST',body,root,{'Idempotency-Key':key}),req('/admin/activity','POST',body,root,{'Idempotency-Key':key})]);
 check('并发幂等创建返回同一活动',duplicate.every(r=>r.code===0)&&duplicate[0].data.activityId===duplicate[1].data.activityId,duplicate.map(r=>({http:r.http,code:r.code})));
 for(const r of duplicate)if(r.data?.activityId&&!activities.includes(r.data.activityId))activities.push(r.data.activityId);
 check('幂等键不能复用到不同内容',(await req('/admin/activity','POST',{...body,title:tag+'不同'},root,{'Idempotency-Key':key})).http===409);
 const ticket=(await req(path+'/submit-review','POST',undefined,a.token)).data;const snap=(await req('/admin/reviews/'+ticket.reviewId+'/snapshot')).data;
 check('审批快照保留提交内容',snap.content.activity.title===tag+'新版'&&snap.hash?.length===64);
 await req('/super/reviews/'+ticket.reviewId+'/decision','PUT',{decision:'REJECTED',comment:'用于修订测试'});
 const version=(await req(path)).data.activity.version;await req(path,'PUT',{...body,title:tag+'再修订',version});
 const after=(await req('/admin/reviews/'+ticket.reviewId+'/snapshot')).data;
 check('改稿不覆盖旧审批内容',after.content.activity.title===snap.content.activity.title&&after.hash===snap.hash);
 check('审批列表显示历史提交标题',(await req('/admin/reviews?decision=REJECTED')).data.records.some(r=>r.reviewId===ticket.reviewId&&r.activityTitle===tag+'新版'));
 const privateFile=await upload('PRIVATE'),publicFile=await upload('PUBLIC');
 const pinned=await upload('PRIVATE');const pinnedId=pinned.data.url.split('/').at(-1);
 const pinActivity=await create({attachments:[{fileName:'历史审批附件.pdf',url:pinned.data.url,fileSize:42}]});
 const pinTicket=(await req('/admin/activity/'+pinActivity+'/submit-review','POST',undefined,a.token)).data.reviewId;
 await req('/super/reviews/'+pinTicket+'/decision','PUT',{decision:'REJECTED',comment:'改稿移除附件'});
 await req('/admin/activity/'+pinActivity,'PUT',{...body,version:(await req('/admin/activity/'+pinActivity)).data.activity.version,attachments:[]});

 check('私有文件上传成功',privateFile.code===0);check('私有文件匿名不能下载',(await fetch(base+privateFile.data.url.replace('/api/v1',''))).status===401);
 check('私有文件同组织可下载',(await fetch(base+privateFile.data.url.replace('/api/v1',''),{headers:{Authorization:'Bearer '+a.token}})).status===200);
 check('私有文件不能通过public别名下载',(await fetch(base+'/file-assets/public/'+assets[0])).status===404);
 check('公开文件可匿名下载',(await fetch(base+publicFile.data.url.replace('/api/v1',''))).status===200);
 const privateKey=sql(`SELECT storage_key FROM t_file_asset WHERE asset_id='${assets[0]}'`);check('存储路径不能绕过文件权限',(await fetch(base+'/files/'+privateKey)).status===404);
 const large=await upload('PRIVATE',3*1024*1024);check('配额内上传成功',large.code===0);const over=await upload('PRIVATE',3*1024*1024);check('组织累计配额生效',over.code!==0&&over.message.includes('空间'));
 const parallelId=await create({quota:20});await req('/admin/activity/'+parallelId+'/publish','POST');const parallelGuid=(await req('/admin/activity/'+parallelId)).data.activity.signupGuid;
 sql("DELETE FROM t_rate_limit WHERE bucket_key LIKE 'signup-%'");
 const parallel=await Promise.allSettled(Array.from({length:10},async(_,index)=>{const response=await fetch(base+'/signup',{method:'POST',headers:{'Content-Type':'application/json','Idempotency-Key':randomUUID()},body:JSON.stringify({guid:parallelGuid,name:'并发报名'+index,phone:'13900001'+String(index).padStart(3,'0'),collegeCode:sql('SELECT college_code FROM t_college LIMIT 1')}),signal:AbortSignal.timeout(8000)});return response.json()}));
 check('小连接池下十路幂等报名不会自锁',parallel.every(item=>item.status==='fulfilled'&&item.value.code===0),{fulfilled:parallel.filter(item=>item.status==='fulfilled').length,codes:parallel.map(item=>item.status==='fulfilled'?item.value.code:item.reason.message)});
 check('十路并发报名落库和计数一致',Number(sql(`SELECT COUNT(*) FROM t_registration WHERE activity_id=${parallelId}`))===10&&Number(sql(`SELECT signed_count FROM t_activity WHERE activity_id=${parallelId}`))===10);
 sql("DELETE FROM t_rate_limit WHERE bucket_key LIKE 'signup-%'");
 const regId=await create({checkinPolicy:'IDENTITY_REQUIRED'});await req('/admin/activity/'+regId+'/publish','POST');const regGuid=(await req('/admin/activity/'+regId)).data.activity.signupGuid;
 const college=sql('SELECT college_code FROM t_college LIMIT 1'),phone='13912345678';const signup={guid:regGuid,name:'身份验收',phone,collegeCode:college};
 check('身份策略拒绝未验证报名',(await req('/signup','POST',signup,'')).code!==0);
 check('身份策略拒绝伪造凭据',(await req('/signup','POST',{...signup,identityProof:'forged'},'')).code!==0);
 check('身份策略拒绝其他手机号凭据',(await req('/signup','POST',{...signup,identityProof:proof('13900000000')},'')).code!==0);
 check('身份策略拒绝过期凭据',(await req('/signup','POST',{...signup,identityProof:proof(phone,{exp:Math.floor(Date.now()/1000)-30})},'')).code!==0);
 check('身份策略接受受信网关签发的测试凭据',(await req('/signup','POST',{...signup,identityProof:proof(phone)},'')).code===0);
 sql(`UPDATE t_activity SET status='ONGOING',start_time=DATE_SUB(NOW(),INTERVAL 1 HOUR),signup_deadline=DATE_SUB(NOW(),INTERVAL 2 HOUR),end_time=DATE_ADD(NOW(),INTERVAL 2 HOUR),checkin_enabled=1 WHERE activity_id=${regId}`);
 check('身份策略拒绝匿名代签',(await req('/checkin','POST',{activityId:regId,phone},'')).code!==0);
 check('身份策略允许验证后签到',(await req('/checkin','POST',{activityId:regId,phone,identityProof:proof(phone)},'')).code===0);
 sql(`UPDATE t_activity SET checkin_policy='STRICT' WHERE activity_id=${regId}`);
 check('严格模式不会降级为基本签到',(await req('/checkin','POST',{activityId:regId,phone:'13900000000',identityProof:proof('13900000000')},'')).code!==0);
 check('分页名单返回页结构且受上限约束',(await req('/admin/activity/'+regId+'/registrations/page?size=1')).data.records.length===1&&(await req('/admin/activity/'+regId+'/registrations/page?size=101')).code!==0);
 check('签到汇总只返回计数',(await req('/admin/activity/'+regId+'/attendance-summary')).data.checkinCount===1);
 check('账号与组织支持分页',(await req('/super/admin/accounts/page?size=1')).data.records.length===1&&(await req('/super/organizations/page?size=1')).data.records.length===1);
 const exported=await raw('/admin/activity/'+regId+'/registrations/export');check('鉴权下载可导出Excel',exported.status===200&&exported.headers.get('content-type').includes('spreadsheet'));
 sql(`UPDATE t_file_asset SET create_time=DATE_SUB(NOW(),INTERVAL 3 DAY) WHERE asset_id IN (${assets.map(id=>"'"+id+"'").join(',')})`);check('未引用文件可受控清理',(await req('/super/file-assets/cleanup','POST')).data.removed>=assets.length-1);
 check('审批快照仍引用的文件不会被清理',(await req('/file-assets/'+pinnedId)).http===200);
 check('清理后文件不再可读',(await req(privateFile.data.url.replace('/api/v1',''))).http===404);
 const logs=await req('/super/audit-logs?opType=LOGIN_FAILURE');check('认证失败有审计记录',logs.data.total>0);
}catch(e){check('完整执行企业验收',false,{error:e.stack})}finally{
 for(const id of activities)await req('/admin/activity/'+id,'DELETE');for(const id of accounts)await req('/super/admin/accounts/'+id+'/status','PUT',{status:0});
 const summary={total:results.length,passed:results.filter(x=>x.pass).length,failed:results.filter(x=>!x.pass).length};fs.writeFileSync('docs/testing/enterprise-results.json',JSON.stringify({tag,time:new Date().toISOString(),summary,results},null,2));console.log(JSON.stringify({summary,failures:results.filter(x=>!x.pass)},null,2));if(summary.failed)process.exitCode=1;
}
