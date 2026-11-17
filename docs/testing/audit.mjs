import {base as testBase,testPassword,testUsername} from './legacy-test-env.mjs';
import fs from 'node:fs';
import {spawnSync} from 'node:child_process';
const base=testBase;
const results=[], ids=[];
let token='',userId;
const tag='AUDIT_'+Date.now();
async function req(path,method='GET',body,auth=token){
 const headers=auth?{Authorization:'Bearer '+auth}:{};
 if(body!==undefined)headers['Content-Type']='application/json';
 const r=await fetch(base+path,{method,headers,body:body===undefined?undefined:JSON.stringify(body)});
 const text=await r.text();let json;try{json=JSON.parse(text)}catch{}
 return {status:r.status,json,text};
}
function check(name,ok,evidence){results.push({name,pass:!!ok,evidence});}
async function test(name,path,method,body,expect,auth){const r=await req(path,method,body,auth);check(name,expect(r),{http:r.status,code:r.json?.code,message:r.json?.message});return r.json?.data;}
function sql(statement){
 const pwd=process.env.TEST_DB_PASSWORD;
 const r=spawnSync(process.env.MYSQL_BIN||'mysql',['--host=127.0.0.1','--user='+process.env.TEST_DB_USER,'--database='+process.env.TEST_SCHEMA,'--batch','--skip-column-names','-e',statement],{env:{...process.env,MYSQL_PWD:pwd},encoding:'utf8'});
 if(r.status!==0)throw Error(r.stderr);return r.stdout.trim();
}
const success=r=>r.json?.code===0, reject=r=>r.json?.code!==0;
try{
 await test('匿名后台访问被拒绝','/admin/current','GET',undefined,r=>r.status===401,'');
 await test('伪造token被拒绝','/admin/current','GET',undefined,r=>r.status===401,'invalid');
 await test('缺少登录参数被拒绝','/auth/login','POST',{},reject,'');
 const login=await test('超级管理员登录','/auth/login','POST',{username:testUsername,password:testPassword},success,'');token=login.token;
 await test('当前账号','/admin/current','GET',undefined,success);
 const types=await test('活动类型字典','/dict/activity-type','GET',undefined,success,'');
 const colleges=await test('学院字典','/dict/college','GET',undefined,success,'');
 const accounts=await test('账号列表不返回密码hash','/super/admin/accounts','GET',undefined,r=>success(r)&&!r.json.data.some(a=>a.passwordHash));
 await test('默认密码新建管理员','/super/admin/accounts','POST',{username:tag+'_default',realName:'测试默认密码',dept:'AUDIT',roleCode:'ADMIN'},success);
 await test('显式密码新建管理员','/super/admin/accounts','POST',{username:tag,realName:'测试账号',dept:'AUDIT',passwordHash:'Audit12345',roleCode:'ADMIN'},success);
 const rows=(await req('/super/admin/accounts')).json.data;userId=rows.find(a=>a.username===tag)?.adminId;
 const u=(await req('/auth/login','POST',{username:tag,password:'Audit12345'},'')).json.data.token;
 await test('普通管理员禁止账号管理','/super/admin/accounts','GET',undefined,r=>r.status===403,u);
 const reset=await test('重置密码','/super/admin/accounts/'+userId+'/password','PUT',undefined,success);
 await test('重置后的随机临时密码可登录','/auth/login','POST',{username:tag,password:reset.temporaryPassword},success,'');
 await test('停用测试账号','/super/admin/accounts/'+userId+'/status','PUT',{status:0},success);
 await test('停用账号不能重新登录','/auth/login','POST',{username:tag,password:reset.temporaryPassword},reject,'');
 await test('停用后旧token失效','/admin/activity/list','GET',undefined,r=>r.status===401||r.status===403,u);
 const now=Date.now(),time=offset=>new Date(now+offset*3600000).toISOString().slice(0,19);
 // ISO above uses UTC: choose safely future days to avoid host timezone ambiguity.
 const body={title:tag,typeCode:types[0].typeCode,location:'AUDIT测试地点',lngLat:'108.9,34.2',signupDeadline:time(48),startTime:time(72),endTime:time(96),quota:2,attachments:[{fileName:'audit.pdf',url:'/api/v1/files/audit.pdf',fileSize:10}]};
 await test('空活动参数被拒绝','/admin/activity','POST',{},reject);
 await test('人数为0被拒绝','/admin/activity','POST',{...body,quota:0},reject);
 await test('截止时间晚于开始被拒绝','/admin/activity','POST',{...body,signupDeadline:time(80)},reject);
 const created=await test('创建活动','/admin/activity','POST',body,success);ids.push(created.activityId);const id=created.activityId,guid=created.signupGuid;
 await test('草稿宣传内容不可公开读取','/promo/'+guid,'GET',undefined,reject,'');
 await test('发布活动','/admin/activity/'+id+'/publish','POST',undefined,success);
 await test('重复发布被拒绝','/admin/activity/'+id+'/publish','POST',undefined,reject);
 const promo=await test('公开宣传接口不泄露签到密钥','/promo/'+guid,'GET',undefined,r=>success(r)&&!r.json.data.activity.checkinSecret,'');
 await test('活动列表','/admin/activity/list?keyword='+tag,'GET',undefined,r=>success(r)&&r.json.data.total>=1);
 await test('报名二维码','/admin/activity/'+id+'/qrcode','GET',undefined,success);
 await test('签到配置','/admin/activity/'+id+'/checkin-config','GET',undefined,success);
 await test('保存相同附件','/admin/activity/'+id,'PUT',body,success);
 let detail=(await req('/admin/activity/'+id)).json.data;check('重复保存不增加附件',detail.attachments.length===1,{count:detail.attachments.length});
 await req('/admin/activity/'+id,'PUT',{...body,attachments:[]});detail=(await req('/admin/activity/'+id)).json.data;check('清空附件有效',detail.attachments.length===0,{count:detail.attachments.length});
 const signup=(phone,extra={})=>({guid,name:'测试学生',phone,collegeCode:colleges[0].collegeCode,...extra});
 await test('错误手机号被拒绝','/signup','POST',signup('123'),reject,'');
 await test('错误学院被拒绝','/signup','POST',signup('13900000001',{collegeCode:'INVALID'}),reject,'');
 await test('正常报名','/signup','POST',signup('13900000001'),success,'');
 await test('重复报名被拒绝','/signup','POST',signup('13900000001'),reject,'');
 const concurrent=await Promise.all(['13900000002','13900000003','13900000004'].map(phone=>req('/signup','POST',signup(phone),'')));
 check('并发报名仅剩余1个名额成功',concurrent.filter(success).length===1,{codes:concurrent.map(r=>r.json?.code),messages:concurrent.map(r=>r.json?.message)});
 await test('报名名单手机号脱敏','/admin/activity/'+id+'/registrations','GET',undefined,r=>success(r)&&r.json.data.every(x=>!/^1\d{10}$/.test(x.phone)));
 const regs=(await req('/admin/activity/'+id+'/registrations?maskPhone=false')).json.data;
 await test('满额报名被拒绝','/signup','POST',signup('13900000005'),reject,'');
 await test('签到默认被关闭','/checkin','POST',{activityId:String(id),phone:regs[0].phone},reject,'');
 const localTime=hours=>new Date(Date.now()+(8+hours)*3600000).toISOString().slice(0,19);
 await test('通过接口调整活动为进行中','/admin/activity/'+id,'PUT',{...body,signupDeadline:localTime(-2),startTime:localTime(-1),endTime:localTime(1)},success);
 await test('能够通过接口开启签到','/admin/activity/'+id+'/checkin-enabled','PUT',{enabled:1},success);
 await test('匿名不可获取动态码','/checkin/qrcode/'+id,'GET',undefined,r=>r.status===401,'');
 const board=await test('登录获取动态码','/checkin/qrcode/'+id,'GET',undefined,success);
 await test('错误动态码被拒绝','/checkin','POST',{activityId:String(id),phone:regs[0].phone,code:'invalid'},reject,'');
 await test('远离会场签到被拒绝','/checkin','POST',{activityId:String(id),phone:regs[0].phone,code:board.code,lngLat:'0,0',device:'audit'},reject,'');
 await test('准确签到缺少定位与设备应拒绝','/checkin','POST',{activityId:String(id),phone:regs[0].phone,code:board.code},reject,'');
 await test('正常准确签到','/checkin','POST',{activityId:String(id),phone:regs[0].phone,code:board.code,lngLat:'108.9,34.2',device:'audit-'+tag},success,'');
 await test('重复签到被拒绝','/checkin','POST',{activityId:String(id),phone:regs[0].phone},reject,'');
 await test('基本签到','/checkin','POST',{activityId:String(id),phone:regs[1].phone},success,'');
 await test('签到记录','/admin/activity/'+id+'/checkins','GET',undefined,r=>success(r)&&r.json.data.length===2);
 for(const path of ['registrations/export','checkins/export']){
 const r=await fetch(base+`/admin/activity/${id}/${path}`,{headers:{Authorization:'Bearer '+token}});const b=Buffer.from(await r.arrayBuffer());check('Excel导出 '+path,r.status===200&&b[0]===80&&b[1]===75,{status:r.status,bytes:b.length});fs.writeFileSync('docs/testing/'+path.split('/')[0]+'.xlsx',b);
 }
 const before=await test('统计报表','/admin/statistics/overview','GET',undefined,success);
 await test('缺少签到参数应返回参数错误','/checkin','POST',{},r=>r.json?.code===40001,'');
 await test('错误日期应返回参数错误','/admin/activity','POST',{...body,startTime:'bad-date'},r=>r.json?.code===40001);
 const form=new FormData();form.append('file',new Blob(['%PDF-1.4\nAUDIT harmless upload probe'],{type:'application/pdf'}),'audit.pdf');
 const upload=await fetch(base+'/file/upload',{method:'POST',body:form});const uploadJson=await upload.json();check('未登录禁止上传',upload.status===401||upload.status===403,{http:upload.status,code:uploadJson.code,message:uploadJson.message});
 const authorizedUpload=await fetch(base+'/file/upload',{method:'POST',headers:{Authorization:'Bearer '+token},body:form});const authorizedJson=await authorizedUpload.json();check('已登录上传合法PDF成功',authorizedJson.code===0,{http:authorizedUpload.status,code:authorizedJson.code,message:authorizedJson.message});
 await test('删除活动','/admin/activity/'+id,'DELETE',undefined,success);
 await test('删除后详情不可访问','/admin/activity/'+id,'GET',undefined,reject);
 await test('删除后宣传不可访问','/promo/'+guid,'GET',undefined,reject,'');
 const after=(await req('/admin/statistics/overview')).json.data;check('删除后统计口径一致',after.signupCount===before.signupCount-regs.length,{beforeSignup:before.signupCount,afterSignup:after.signupCount,beforeActivity:before.activityCount,afterActivity:after.activityCount});
}catch(e){check('测试脚本完整执行',false,{error:e.message});process.exitCode=1;}
finally{
 // Remove only child data created by this run; keep canceled activities and disabled audit accounts for traceability.
 for(const id of ids){try{sql(`DELETE k FROM t_checkin k JOIN t_activity a ON a.activity_id=k.activity_id WHERE a.activity_id=${id} AND a.title='${tag}'; DELETE r FROM t_registration r JOIN t_activity a ON a.activity_id=r.activity_id WHERE a.activity_id=${id} AND a.title='${tag}'; UPDATE t_activity SET signed_count=0,deleted=1,status='CANCELED' WHERE activity_id=${id} AND title='${tag}';`)}catch(e){check('测试数据清理',false,{error:e.message});}}
 try{sql(`UPDATE t_admin SET status=0,token_version=token_version+1 WHERE username IN ('${tag}','${tag}_default')`)}catch(e){check('测试账号清理',false,{error:e.message});}
 fs.writeFileSync('docs/testing/results.json',JSON.stringify({tag,base,time:new Date().toISOString(),fixtureIds:ids,fixtureAdminId:userId,summary:{total:results.length,passed:results.filter(r=>r.pass).length,failed:results.filter(r=>!r.pass).length},results},null,2));
 console.log(JSON.stringify({total:results.length,passed:results.filter(r=>r.pass).length,failures:results.filter(r=>!r.pass)},null,2));
 if(results.some(r=>!r.pass))process.exitCode=1;
}
