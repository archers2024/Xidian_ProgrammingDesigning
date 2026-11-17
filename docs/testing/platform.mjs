import {base as testBase,testPassword,testUsername} from './legacy-test-env.mjs';
import fs from 'node:fs';
const base=testBase,tag='PLAT_'+Date.now(),results=[],orgs=[],accounts=[],activities=[];
let root='';
async function req(route,method='GET',body,token=root){const r=await fetch(base+route,{method,headers:{Authorization:'Bearer '+token,...(body===undefined?{}:{'Content-Type':'application/json'})},body:body===undefined?undefined:JSON.stringify(body)});let json;try{json=await r.json()}catch{}return{http:r.status,...json};}
function check(name,pass,evidence){results.push({name,pass:!!pass,evidence});}
const time=h=>new Date(Date.now()+(8+h)*3600000).toISOString().slice(0,19);
try{
 root=(await req('/auth/login','POST',{username:testUsername,password:testPassword},'')).data.token;
 for(const suffix of ['A','B']){
  const org=await req('/super/organizations','POST',{code:tag+suffix,name:'测试组织'+suffix});if(org.code!==0)throw Error(org.message);orgs.push(org.data.orgId);
  const account=await req('/super/admin/accounts','POST',{username:tag+suffix,realName:'测试管理员',passwordHash:'Platform12345',roleCode:'ADMIN',orgId:org.data.orgId});check('创建组织管理员'+suffix,account.code===0);
 }
 const all=(await req('/super/admin/accounts')).data;accounts.push(...all.filter(a=>a.username.startsWith(tag)).map(a=>a.adminId));
 const a=(await req('/auth/login','POST',{username:tag+'A',password:'Platform12345'},'')).data.token;
 const b=(await req('/auth/login','POST',{username:tag+'B',password:'Platform12345'},'')).data.token;
 const body={title:tag,typeCode:'LECTURE',location:'测试场地',quota:10,signupDeadline:time(1),startTime:time(2),endTime:time(3)};
 const created=await req('/admin/activity','POST',body,a);if(created.code!==0)throw Error(created.message);const id=created.data.activityId,guid=created.data.signupGuid;activities.push(id);
 check('创建活动自动绑定组织',(await req('/admin/activity/'+id,'GET',undefined,a)).data.activity.orgId===orgs[0]);
 const listA=await req('/admin/activity/list?keyword='+tag,'GET',undefined,a),listB=await req('/admin/activity/list?keyword='+tag,'GET',undefined,b);
 check('组织A可查看本组织活动',listA.data.total===1);check('组织B列表隔离',listB.data.total===0);
 for(const [method,path,payload] of [['GET','',undefined],['PUT','',body],['DELETE','',undefined],['POST','/publish',undefined],['PUT','/checkin-enabled',{enabled:1}],['GET','/registrations',undefined],['GET','/checkins',undefined],['GET','/registrations/export',undefined],['GET','/checkins/export',undefined],['GET','/qrcode',undefined],['GET','/checkin-config',undefined],['GET','/reviews',undefined],['POST','/submit-review',undefined]]){
  const r=await req('/admin/activity/'+id+path,method,payload,b);check('跨组织拒绝 '+method+' '+path,r.http===403,{http:r.http,code:r.code});
 }
 check('拒绝伪造组织创建',(await req('/admin/activity','POST',{...body,orgId:orgs[1]},a)).code!==0);
 check('拒绝活动转移组织',(await req('/admin/activity/'+id,'PUT',{...body,orgId:orgs[1]},root)).code!==0);
 check('拒绝跨组织动态码',(await req('/checkin/qrcode/'+id,'GET',undefined,b)).http===403);
 check('普通管理员不能管理组织',(await req('/super/organizations','GET',undefined,a)).http===403);
 check('普通管理员不能看平台审计',(await req('/super/audit-logs','GET',undefined,a)).http===403);
 check('组织可见列表隔离',(await req('/admin/organizations','GET',undefined,a)).data.every(o=>o.orgId===orgs[0]));
 check('组织B统计不含其他组织',(await req('/admin/statistics/overview','GET',undefined,b)).data.activityCount===0);
 check('组织A统计包含本组织',(await req('/admin/statistics/overview','GET',undefined,a)).data.activityCount===1);
 check('新组织默认要求审核',(await req('/admin/activity/'+id+'/publish','POST',undefined,a)).code!==0);
 const ticket=await req('/admin/activity/'+id+'/submit-review','POST',undefined,a);
 await req('/super/reviews/'+ticket.data.reviewId+'/decision','PUT',{decision:'APPROVED'});
 await req('/admin/activity/'+id+'/publish','POST',undefined,a);
 check('公开活动可读取',(await req('/promo/'+guid,'GET',undefined,'')).code===0);
 await req('/super/organizations/'+orgs[0]+'/status','PUT',{status:0});
 check('组织停用撤销访问',(await req('/admin/current','GET',undefined,a)).http===401);
 check('组织停用拒绝登录',(await req('/auth/login','POST',{username:tag+'A',password:'Platform12345'},'')).code!==0);
 check('组织停用关闭宣传',(await req('/promo/'+guid,'GET',undefined,'')).code!==0);
 check('组织停用关闭报名',(await req('/signup','POST',{guid,name:'测试学生',phone:'13912345678',collegeCode:'CS'},'')).message.includes('组织'));
 check('组织停用关闭签到',(await req('/checkin','POST',{activityId:id,phone:'13912345678'},'')).message.includes('组织'));
 await req('/super/organizations/'+orgs[0]+'/status','PUT',{status:1});
 check('重新启用不恢复旧令牌',(await req('/admin/current','GET',undefined,a)).http===401);
 check('审计日志可分页读取',(await req('/super/audit-logs?size=5')).data.records.length<=5);
 check('默认组织不可停用',(await req('/super/organizations/1/status','PUT',{status:0})).code!==0);
 check('重复组织编码拒绝',(await req('/super/organizations','POST',{code:tag+'A',name:'重复'})).code!==0);
 check('拒绝小数组织状态',(await req('/super/organizations/'+orgs[1]+'/status','PUT',{status:0.5})).code===40001);
}catch(e){check('完整执行平台测试',false,{error:e.message});}
finally{
 for(const id of activities)await req('/admin/activity/'+id,'DELETE');
 for(const id of accounts)await req('/super/admin/accounts/'+id+'/status','PUT',{status:0});
 for(const id of orgs)await req('/super/organizations/'+id+'/status','PUT',{status:0});
 const output={time:new Date().toISOString(),tag,summary:{total:results.length,passed:results.filter(x=>x.pass).length,failed:results.filter(x=>!x.pass).length},results};fs.writeFileSync('docs/testing/platform-results.json',JSON.stringify(output,null,2));console.log(JSON.stringify({...output.summary,failures:results.filter(x=>!x.pass)},null,2));if(output.summary.failed)process.exitCode=1;
}
