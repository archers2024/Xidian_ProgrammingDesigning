import {base as testBase,testPassword,testUsername} from './legacy-test-env.mjs';
import fs from 'node:fs';
import {spawnSync} from 'node:child_process';
const base=testBase,tag='ROUND2_'+Date.now(),results=[],activities=[],accounts=[];
let token='';
async function req(path,method='GET',body,auth=token){const h=auth?{Authorization:'Bearer '+auth}:{};if(body!==undefined)h['Content-Type']='application/json';const r=await fetch(base+path,{method,headers:h,body:body===undefined?undefined:JSON.stringify(body)});const text=await r.text();let json;try{json=JSON.parse(text)}catch{}return {status:r.status,json,text};}
function check(name,pass,evidence){results.push({name,pass:!!pass,evidence});}
const ok=r=>r.json?.code===0,bad=r=>r.status===400||(r.json?.code===40001)||(r.json?.code===40002)||(r.json?.code===40003);
async function test(name,path,method,body,expect=ok,auth){const r=await req(path,method,body,auth);check(name,expect(r),{http:r.status,code:r.json?.code,message:r.json?.message});return r;}
const time=h=>new Date(Date.now()+(h+8)*3600000).toISOString().slice(0,19);
try{
 token=(await req('/auth/login','POST',{username:testUsername,password:testPassword},'')).json.data.token;
 for(const path of ['/admin;probe/activity/list','/%61dmin/activity/list','/super;probe/admin/accounts','/file;probe/upload','/checkin;probe/qrcode/1']){
  await test('路径鉴权 '+path,path,path.includes('upload')?'POST':'GET',undefined,r=>r.status===401||r.status===400,'');
 }
 for(const body of [{},{username:'x',realName:'测试'},{username:tag,realName:'测试',roleCode:'ROOT'},{username:tag,realName:'测试',passwordHash:'short'}])await test('账号输入校验 '+JSON.stringify(body),'/super/admin/accounts','POST',body,bad);
 await test('最后超管不能停用','/super/admin/accounts/1/status','PUT',{status:0},bad);
 await test('状态取值校验','/super/admin/accounts/1/status','PUT',{status:5},bad);
 await test('创建第二轮测试账号','/super/admin/accounts','POST',{username:tag,realName:'二轮测试',passwordHash:'Round212345'},ok);
 let list=(await req('/super/admin/accounts')).json.data;let aid=list.find(a=>a.username===tag).adminId;accounts.push(aid);
 let u=(await req('/auth/login','POST',{username:tag,password:'Round212345'},'')).json.data.token;
 await test('普通账号不能查看原手机号','/admin/activity/1/registrations?maskPhone=false','GET',undefined,bad,u);
 await test('普通账号正确输入原密码才可修改','/admin/password','PUT',{oldPassword:'wrong',newPassword:'NewRound2123'},bad,u);
 await test('新密码策略','/admin/password','PUT',{oldPassword:'Round212345',newPassword:'123'},bad,u);
 await test('修改自身密码','/admin/password','PUT',{oldPassword:'Round212345',newPassword:'NewRound2123'},ok,u);
 await test('修改密码撤销旧令牌','/admin/current','GET',undefined,r=>r.status===401,u);
 u=(await req('/auth/login','POST',{username:tag,password:'NewRound2123'},'')).json.data.token;
 await test('退出登录','/auth/logout','POST',undefined,ok,u);
 await test('退出撤销令牌','/admin/current','GET',undefined,r=>r.status===401,u);
 const reset=await test('重置二轮账号密码','/super/admin/accounts/'+aid+'/password','PUT',undefined);
 u=(await req('/auth/login','POST',{username:tag,password:reset.json.data.temporaryPassword},'')).json.data.token;
 await test('临时密码不能访问活动','/admin/activity/list','GET',undefined,r=>r.status===403,u);
 await test('临时密码可修改','/admin/password','PUT',{oldPassword:reset.json.data.temporaryPassword,newPassword:'FinalRound2123'},ok,u);
 u=(await req('/auth/login','POST',{username:tag,password:'FinalRound2123'},'')).json.data.token;
 await test('修改临时密码后恢复访问','/admin/activity/list','GET',undefined,ok,u);
 for(const q of ['size=-1','size=100000','current=0','from=bad'])await test('分页与日期边界 '+q,'/admin/activity/list?'+q,'GET',undefined,bad);
 for(const days of [0,-1,367])await test('统计天数边界 '+days,'/admin/statistics/overview?days='+days,'GET',undefined,bad);
 const types=(await req('/dict/activity-type')).json.data,colleges=(await req('/dict/college')).json.data;
 const body={title:tag,typeCode:types[0].typeCode,location:'第二轮测试地点',lngLat:'108.9,34.2',signupDeadline:time(1),startTime:time(2),endTime:time(3),quota:5};
 for(const patch of [{typeCode:'INVALID'},{location:''},{title:123},{quota:1.5},{checkinRadius:-1},{lngLat:'NaN,0'},{lngLat:'Infinity,0'},{lngLat:'181,0'},{lngLat:'0,91'},{lngLat:'1,'},{lngLat:'1,2,3'},{startTime:'2027-02-30 12:00:00'},{endTime:time(1)}])await test('活动边界 '+JSON.stringify(patch),'/admin/activity','POST',{...body,...patch},bad);
 const created=await req('/admin/activity','POST',body);if(!ok(created))throw Error('fixture create failed');const id=created.json.data.activityId,guid=created.json.data.signupGuid;activities.push(id);
 await req('/admin/activity/'+id+'/publish','POST');
 await test('未开始活动不能开启签到','/admin/activity/'+id+'/checkin-enabled','PUT',{enabled:1},bad);
 for(let n=1;n<=3;n++)await test('二轮报名 '+n,'/signup','POST',{guid,name:'测试学生',phone:'1380000000'+n,collegeCode:colleges[0].collegeCode},ok,'');
 await req('/admin/activity/'+id,'PUT',{...body,signupDeadline:time(-2),startTime:time(-1),endTime:time(1)});
 await test('二轮开启签到','/admin/activity/'+id+'/checkin-enabled','PUT',{enabled:1});
 let board=(await req('/checkin/qrcode/'+id)).json.data;
 for(const patch of [{lngLat:''},{device:''},{lngLat:'NaN,34.2'},{lngLat:'181,34.2'},{code:'expired-code'}])await test('准确签到边界 '+JSON.stringify(patch),'/checkin','POST',{activityId:id,phone:'13800000001',code:board.code,lngLat:'108.9,34.2',device:tag,...patch},bad,'');
 const sameDevice=await Promise.all(['13800000001','13800000002'].map(phone=>req('/checkin','POST',{activityId:id,phone,code:board.code,lngLat:'108.9,34.2',device:tag},'')));
 check('并发相同设备仅一次成功',sameDevice.filter(ok).length===1,{codes:sameDevice.map(x=>x.json?.code)});
 const repeat=await Promise.all([1,2,3].map(()=>req('/checkin','POST',{activityId:id,phone:'13800000003'},'')));
 check('并发同一报名仅一次成功',repeat.filter(ok).length===1,{codes:repeat.map(x=>x.json?.code)});
 await req('/admin/activity/'+id,'PUT',{...body,signupDeadline:time(-3),startTime:time(-2),endTime:time(-1)});
 await test('活动结束动态码不可读','/checkin/qrcode/'+id,'GET',undefined,bad);
 await test('活动结束签到拒绝','/checkin','POST',{activityId:id,phone:'13800000002'},bad,'');
 const stat=(await req('/admin/statistics/overview')).json.data;
 check('统计分类总报名可对账',stat.typeStats.reduce((n,x)=>n+x.signupCount,0)===stat.signupCount,{total:stat.signupCount});
 check('统计分类总签到可对账',stat.typeStats.reduce((n,x)=>n+x.checkinCount,0)===stat.checkinCount,{total:stat.checkinCount});
 for(const [name,contents] of [['probe.exe','%PDF-1.4'],['probe.pdf',''],['spoof.jpg','not an image']]){
  const fd=new FormData();fd.append('file',new Blob([contents]),name);const r=await fetch(base+'/file/upload',{method:'POST',headers:{Authorization:'Bearer '+token},body:fd});const j=await r.json();check('拒绝非法文件 '+name,j.code===40001,{http:r.status,code:j.code});
 }
 const fd=new FormData();fd.append('file',new Blob(['%PDF-1.4\nRound2 probe']),tag+'.pdf');let r=await fetch(base+'/file/upload',{method:'POST',headers:{Authorization:'Bearer '+token},body:fd});let j=await r.json();check('二轮合法文件上传',j.code===0,{code:j.code});
 if(j.data?.url){r=await fetch(base+j.data.url.replace('/api/v1',''),{headers:{Authorization:'Bearer '+token}});check('上传返回资源可读取',r.status===200&&(await r.text()).startsWith('%PDF-'),{http:r.status});}
 const final=(await req('/super/admin/accounts')).json.data;check('新旧账号均不返回密码散列',final.every(a=>!Object.hasOwn(a,'passwordHash')));
}catch(e){check('完整执行第二轮',false,{error:e.message});}
finally{
 for(const id of activities)await req('/admin/activity/'+id,'DELETE');
 for(const id of accounts)await req('/super/admin/accounts/'+id+'/status','PUT',{status:0});
 const pwd=fs.readFileSync('backend/src/main/resources/application.yml','utf8').match(/^    password: (.+)$/m)[1].trim();
 for(const id of activities){spawnSync('E:/mysql8.0/mysql-8.0.46-winx64/bin/mysql.exe',['--host=127.0.0.1','--user=campus_app','--database=campus_activity','-e',`DELETE k FROM t_checkin k JOIN t_activity a ON k.activity_id=a.activity_id WHERE a.activity_id=${id} AND a.title='${tag}'; DELETE r FROM t_registration r JOIN t_activity a ON r.activity_id=a.activity_id WHERE a.activity_id=${id} AND a.title='${tag}'; UPDATE t_activity SET signed_count=0 WHERE activity_id=${id} AND title='${tag}'`],{env:{...process.env,MYSQL_PWD:pwd}});}
 const output={tag,time:new Date().toISOString(),summary:{total:results.length,passed:results.filter(r=>r.pass).length,failed:results.filter(r=>!r.pass).length},results};
 fs.writeFileSync('docs/testing/round2-results.json',JSON.stringify(output,null,2));console.log(JSON.stringify({...output.summary,failures:results.filter(r=>!r.pass)},null,2));if(output.summary.failed)process.exitCode=1;
}
