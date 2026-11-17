import {base,testPassword,testUsername} from './test-env.mjs';
import fs from 'node:fs';
import {spawnSync} from 'node:child_process';
import {createHmac,createHash} from 'node:crypto';
const checks=[],ids=[],tag='TOPIC2_'+Date.now();let token='',org;
const check=(name,pass,evidence)=>checks.push({name,pass:!!pass,evidence});
function sql(statement){const r=spawnSync(process.env.MYSQL_BIN,['--host=127.0.0.1','--default-character-set=utf8mb4','--user='+process.env.TEST_DB_USER,'--database='+process.env.TEST_SCHEMA,'--batch','--skip-column-names','-e',statement],{env:{...process.env,MYSQL_PWD:process.env.TEST_DB_PASSWORD},encoding:'utf8'});if(r.status!==0)throw Error(r.stderr);return r.stdout.trim()}
async function req(route,method='GET',body,auth=token){const r=await fetch(base+route,{method,headers:{...(auth?{Authorization:'Bearer '+auth}:{}),...(body?{'Content-Type':'application/json'}:{})},body:body?JSON.stringify(body):undefined,signal:AbortSignal.timeout(15000)});return{http:r.status,...await r.json()}}
const time=h=>new Date(Date.now()+(8+h)*3600000).toISOString().slice(0,19);
const content={title:tag,typeCode:'LECTURE',introduction:'题目二逐条验收',location:'学生活动中心',quota:2,signupDeadline:time(1),startTime:time(2),endTime:time(3),liveUrl:'https://example.org/live',checkinPolicy:'BASIC',lngLat:'108.9,34.2'};
async function create(extra={}){const r=await req('/admin/activity','POST',{...content,orgId:org,...extra});if(r.code!==0)throw Error(r.message);ids.push(r.data.activityId);return r.data.activityId}
async function detail(id){return(await req('/admin/activity/'+id)).data.activity}
async function edit(id,extra={}){const a=await detail(id);return req('/admin/activity/'+id,'PUT',{...content,orgId:org,...a,...extra,version:a.version})}
function clearAdmission(){sql("DELETE FROM t_rate_limit WHERE bucket_key LIKE 'signup-%'")}
try{
 token=(await req('/auth/login','POST',{username:testUsername,password:testPassword},'')).data.token;
 org=(await req('/super/organizations','POST',{code:tag,name:'题目二专项组织'})).data.orgId;
 const college=sql('SELECT college_code FROM t_college LIMIT 1');
 const sample=await create();const draft=await detail(sample);
 check('创建包含必填字段、直播链接，初始未发布',draft.status==='DRAFT'&&draft.title===tag&&draft.liveUrl===content.liveUrl&&draft.publishTime===null);
 check('未发布活动不允许匿名报名',(await req('/signup','POST',{guid:draft.signupGuid,name:'验收学生',phone:'13911110000',collegeCode:college},'')).code!==0);
 await req('/super/organizations/'+org+'/review-policy','PUT',{requireReview:false});
 check('发布活动成功',(await req('/admin/activity/'+sample+'/publish','POST')).code===0);
 const published=await detail(sample),guid=published.signupGuid;
 check('发布时间持久化并随活动列表返回',!!published.publishTime&&(await req('/admin/activity/list?keyword='+tag)).data.records.some(a=>a.activityId===sample&&a.publishTime===published.publishTime));
 const promo=(await req('/promo/'+guid,'GET',undefined,'')).data;
 check('匿名可查看宣传信息和报名时效',promo.activity.title===tag&&promo.signupOpen===true);
 clearAdmission();
 for(const [name,phone,collegeCode] of [['','13911110000',college],['验收学生','123',college],['验收学生','13911110000','INVALID']])check('报名字段验证 '+JSON.stringify({name,phone,collegeCode}),(await req('/signup','POST',{guid,name,phone,collegeCode},'')).code!==0);
 const signup=phone=>req('/signup','POST',{guid,name:'验收学生',phone,collegeCode:college},'');
 check('参与人无需注册登录即可报名',(await signup('13911110000')).code===0);
 check('手机号去重',(await signup('13911110000')).code!==0);
 check('剩余名额可报名',(await signup('13911110001')).code===0);
 check('满额禁止继续报名',(await signup('13911110002')).code!==0);
 const previous=sql(`SELECT reg_id,name,phone,college_code FROM t_registration WHERE activity_id=${sample} ORDER BY reg_id`);
 await edit(sample,{title:tag+'修订',introduction:'宣传页修订内容',quota:1});
 const revised=(await req('/promo/'+guid,'GET',undefined,'')).data;
 check('编辑自动更新原链接宣传内容并保留报名人员',revised.activity.title===tag+'修订'&&revised.activity.introduction==='宣传页修订内容'&&sql(`SELECT reg_id,name,phone,college_code FROM t_registration WHERE activity_id=${sample} ORDER BY reg_id`)===previous);
 check('缩减名额不丢报名计数',(await detail(sample)).signedCount===2&&(await detail(sample)).quota===2);
 const bodyForState={DRAFT:[1,2,3],OPEN:[1,2,3],CLOSED:[-1,1,2],ONGOING:[-2,-1,1],FINISHED:[-3,-2,-1],CANCELED:[1,2,3]};
 await req('/super/organizations/'+org+'/review-policy','PUT',{requireReview:true});
 for(const [state,offset] of Object.entries(bodyForState)){
  const id=await create();sql(`UPDATE t_activity SET status='${state}',approval_status='APPROVED',signup_deadline='${time(offset[0])}',start_time='${time(offset[1])}',end_time='${time(offset[2])}' WHERE activity_id=${id}`);
  const original=await detail(id);const result=await edit(id,{title:tag+state});
  check(state+'状态可编辑且原链接与生命周期不丢失',result.code===0&&(await detail(id)).signupGuid===original.signupGuid&&(await detail(id)).status===state,result);
  check(state+'状态可删除',(await req('/admin/activity/'+id,'DELETE')).code===0);
  check(state+'删除后不可访问或报名',(await req('/admin/activity/'+id)).code!==0&&(await req('/promo/'+original.signupGuid,'GET',undefined,'')).code!==0);
 }
 const pending=await create();const ticket=(await req('/admin/activity/'+pending+'/submit-review','POST')).data.reviewId;
 const snapshot=(await req('/admin/reviews/'+ticket+'/snapshot')).data;
 check('待审时仍可编辑',(await edit(pending,{title:tag+'待审新稿'})).code===0);
 check('待审旧申请自动撤销且不可再批准',(await req('/admin/activity/'+pending+'/reviews')).data.some(r=>r.reviewId===ticket&&r.decision==='CANCELED')&&(await req('/super/reviews/'+ticket+'/decision','PUT',{decision:'APPROVED'})).code!==0);
 check('旧申请不可变快照保留',(await req('/admin/reviews/'+ticket+'/snapshot')).data.hash===snapshot.hash);
 await req('/super/organizations/'+org+'/review-policy','PUT',{requireReview:false});
 const basic=await create({quota:5});await req('/admin/activity/'+basic+'/publish','POST');const bg=(await detail(basic)).signupGuid;
 clearAdmission();for(const phone of ['13922220000','13922220001'])await req('/signup','POST',{guid:bg,name:'签到验收',phone,collegeCode:college},'');
 const ongoing={signupDeadline:time(-2),startTime:time(-1),endTime:time(1)};
 await edit(basic,ongoing);
 check('管理员可开启进行中活动签到',(await req('/admin/activity/'+basic+'/checkin-enabled','PUT',{enabled:1})).code===0);
 check('基本签到无需参与人账号',(await req('/checkin','POST',{activityId:basic,phone:'13922220000'},'')).code===0);
 check('重复签到拒绝',(await req('/checkin','POST',{activityId:basic,phone:'13922220000'},'')).code!==0);
 await edit(basic,{...ongoing,checkinPolicy:'ACCURATE_REQUIRED'});
 const board=(await req('/checkin/qrcode/'+basic)).data;
 check('动态码半分钟刷新并提供到期时间',board.timeStep===30&&board.expiresAt>Date.now()&&board.expiresAt-Date.now()<=30000&&board.code.length===8);
 await new Promise(resolve=>setTimeout(resolve,Math.max(0,board.expiresAt-Date.now())+150));
 const rotated=(await req('/checkin/qrcode/'+basic)).data;
 check('实际跨越30秒时间片后动态码更新',rotated.code!==board.code&&rotated.expiresAt>board.expiresAt);
 check('准确签到禁止降级基本签到',(await req('/checkin','POST',{activityId:basic,phone:'13922220001'},'')).code!==0);
 const secret=sql(`SELECT checkin_secret FROM t_activity WHERE activity_id=${basic}`),expired=createHmac('sha1',secret).update(basic+'|'+(Math.floor(Date.now()/30000)-3)).digest('hex').slice(0,8);
 check('过期动态码不能签到',(await req('/checkin','POST',{activityId:basic,phone:'13922220001',code:expired,lngLat:'108.9,34.2',device:'topic2'},'')).code!==0);
 check('异地定位不能签到',(await req('/checkin','POST',{activityId:basic,phone:'13922220001',code:board.code,lngLat:'116.4,39.9',device:'topic2'},'')).code!==0);
 const current=(await req('/checkin/qrcode/'+basic)).data.code;
 check('现场定位和当前动态码准确签到成功',(await req('/checkin','POST',{activityId:basic,phone:'13922220001',code:current,lngLat:'108.9,34.2',device:'topic2'},'')).data?.method==='ACCURATE');
 const regBefore=sql(`SELECT COUNT(*) FROM t_registration WHERE activity_id=${basic}`),ciBefore=sql(`SELECT COUNT(*) FROM t_checkin WHERE activity_id=${basic}`);
 await edit(basic,{...ongoing,title:tag+'签到后改稿'});
 check('签到后编辑保留报名与签到',sql(`SELECT COUNT(*) FROM t_registration WHERE activity_id=${basic}`)===regBefore&&sql(`SELECT COUNT(*) FROM t_checkin WHERE activity_id=${basic}`)===ciBefore);
 await req('/admin/activity/'+basic,'DELETE');
 check('删除保留报名及签到历史数据',Number(regBefore)===2&&Number(ciBefore)===2&&sql(`SELECT COUNT(*) FROM t_registration WHERE activity_id=${basic}`)===regBefore&&sql(`SELECT COUNT(*) FROM t_checkin WHERE activity_id=${basic}`)===ciBefore);
 check('删除后签到关闭',(await req('/checkin','POST',{activityId:basic,phone:'13922220001'},'')).code!==0);
 for(const filter of ['typeCode=LECTURE','status=OPEN',`from=${time(1)}&to=${time(3)}`]){const result=await req('/admin/activity/list?keyword='+tag+'&'+filter);check('组合条件筛选 '+filter,result.code===0&&result.data.records.some(a=>a.activityId===sample))}
}catch(error){check('完整执行题目二专项',false,{error:error.stack})}finally{
 for(const id of ids)try{if((await req('/admin/activity/'+id)).code===0)await req('/admin/activity/'+id,'DELETE')}catch{}
 if(org)await req('/super/organizations/'+org+'/status','PUT',{status:0});
 const summary={total:checks.length,passed:checks.filter(c=>c.pass).length,failed:checks.filter(c=>!c.pass).length};
 fs.writeFileSync('docs/testing/topic2-results.json',JSON.stringify({time:new Date().toISOString(),schema:process.env.TEST_SCHEMA,jarSha256:createHash('sha256').update(fs.readFileSync('backend/target/activity-system-1.0.0.jar')).digest('hex'),summary,checks},null,2));
 console.log(JSON.stringify({summary,failures:checks.filter(c=>!c.pass)},null,2));if(summary.failed)process.exitCode=1;
}
