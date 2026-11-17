import {base as testBase,testPassword,testUsername} from './legacy-test-env.mjs';
import fs from 'node:fs';
const base=testBase,tag='REV_'+Date.now(),results=[],ids=[];let root='',org,account;
async function req(path,method='GET',body,token=root){const r=await fetch(base+path,{method,headers:{Authorization:'Bearer '+token,...(body===undefined?{}:{'Content-Type':'application/json'})},body:body===undefined?undefined:JSON.stringify(body)});return{http:r.status,...await r.json()}}
function check(name,pass,evidence){results.push({name,pass:!!pass,evidence})}
const date=h=>new Date(Date.now()+(8+h)*3600000).toISOString().slice(0,19);
try{
root=(await req('/auth/login','POST',{username:testUsername,password:testPassword},'')).data.token;
org=(await req('/super/organizations','POST',{code:tag,name:'审批流程测试'})).data.orgId;
await req('/super/admin/accounts','POST',{username:tag,passwordHash:'Review12345',realName:'审批测试员',roleCode:'ADMIN',orgId:org});
account=(await req('/super/admin/accounts')).data.find(x=>x.username===tag).adminId;
const user=(await req('/auth/login','POST',{username:tag,password:'Review12345'},'')).data.token;
const body={title:tag,typeCode:'LECTURE',location:'审批测试场地',quota:10,signupDeadline:date(1),startTime:date(2),endTime:date(3)};
const id=(await req('/admin/activity','POST',body,user)).data.activityId;ids.push(id);
const path='/admin/activity/'+id;
check('默认启用审核',(await req(path,'GET',undefined,user)).data.activity.reviewRequired===true);
check('未审核禁止发布',(await req(path+'/publish','POST',undefined,user)).code!==0);
const submits=await Promise.all([req(path+'/submit-review','POST',undefined,user),req(path+'/submit-review','POST',undefined,user)]);
check('并发提交只成功一次',submits.filter(r=>r.code===0).length===1);
let ticket=submits.find(r=>r.code===0).data.reviewId;
check('审核中允许改稿',(await req(path,'PUT',{...body,title:tag+'待审修订'},user)).code===0);
check('待审改稿撤销旧申请',(await req(path+'/reviews','GET',undefined,user)).data.some(r=>r.reviewId===ticket&&r.decision==='CANCELED'));
check('已撤销旧稿不能审批',(await req('/super/reviews/'+ticket+'/decision','PUT',{decision:'APPROVED'})).code!==0);
ticket=(await req(path+'/submit-review','POST',undefined,user)).data.reviewId;
check('普通管理员不能审批',(await req('/super/reviews/'+ticket+'/decision','PUT',{decision:'APPROVED'},user)).http===403);
check('驳回必须填写原因',(await req('/super/reviews/'+ticket+'/decision','PUT',{decision:'REJECTED',comment:' '})).code!==0);
check('拒绝无效审批状态',(await req('/super/reviews/'+ticket+'/decision','PUT',{decision:'OTHER'})).code!==0);
check('可驳回申请',(await req('/super/reviews/'+ticket+'/decision','PUT',{decision:'REJECTED',comment:'请补充说明'})).code===0);
check('已驳回禁止发布',(await req(path+'/publish','POST',undefined,user)).code!==0);
ticket=(await req(path+'/submit-review','POST',undefined,user)).data.reviewId;
const decisions=await Promise.all([req('/super/reviews/'+ticket+'/decision','PUT',{decision:'APPROVED'}),req('/super/reviews/'+ticket+'/decision','PUT',{decision:'REJECTED',comment:'并发冲突'})]);
check('并发审批只成功一次',decisions.filter(r=>r.code===0).length===1);
let detail=(await req(path,'GET',undefined,user)).data.activity;
if(detail.approvalStatus!=='APPROVED'){ticket=(await req(path+'/submit-review','POST',undefined,user)).data.reviewId;await req('/super/reviews/'+ticket+'/decision','PUT',{decision:'APPROVED'})}
check('已通过再次编辑成功',(await req(path,'PUT',{...body,title:tag+'修订'},user)).code===0);
check('改稿后审批失效',(await req(path,'GET',undefined,user)).data.activity.approvalStatus==='NONE');
check('改稿后禁止直接发布',(await req(path+'/publish','POST',undefined,user)).code!==0);
ticket=(await req(path+'/submit-review','POST',undefined,user)).data.reviewId;
await req('/super/reviews/'+ticket+'/decision','PUT',{decision:'APPROVED'});
check('审核后允许发布',(await req(path+'/publish','POST',undefined,user)).code===0);
check('受控活动发布后仍可改稿',(await req(path,'PUT',body,user)).code===0);
check('历史包含多个审批版本',(await req(path+'/reviews','GET',undefined,user)).data.length>=3);
check('组织审批列表隔离',(await req('/admin/reviews','GET',undefined,user)).data.records.every(r=>r.orgId===org));
check('分页越界拒绝',(await req('/admin/reviews?size=101','GET',undefined,user)).code!==0);
check('普通管理员不能改发布策略',(await req('/super/organizations/'+org+'/review-policy','PUT',{requireReview:false},user)).http===403);
check('发布策略拒绝字符串布尔',(await req('/super/organizations/'+org+'/review-policy','PUT',{requireReview:'false'})).code!==0);
const own=(await req('/admin/activity','POST',{...body,orgId:org})).data.activityId;ids.push(own);
const ownTicket=(await req('/admin/activity/'+own+'/submit-review','POST')).data.reviewId;
check('平台管理员不可自审',(await req('/super/reviews/'+ownTicket+'/decision','PUT',{decision:'APPROVED'})).code!==0);
await req('/admin/activity/'+own,'DELETE');
check('取消活动关闭待审申请',(await req('/admin/reviews?decision=CANCELED')).data.records.some(r=>r.reviewId===ownTicket));
check('已撤销申请不能再审批',(await req('/super/reviews/'+ownTicket+'/decision','PUT',{decision:'APPROVED'})).code!==0);
check('平台可变更发布策略',(await req('/super/organizations/'+org+'/review-policy','PUT',{requireReview:false})).code===0);
const direct=(await req('/admin/activity','POST',body,user)).data.activityId;ids.push(direct);
check('允许直发策略生效',(await req('/admin/activity/'+direct+'/publish','POST',undefined,user)).code===0);
}catch(e){check('完整执行',false,e.stack)}finally{
for(const id of ids)await req('/admin/activity/'+id,'DELETE');
if(account)await req('/super/admin/accounts/'+account+'/status','PUT',{status:0});if(org)await req('/super/organizations/'+org+'/status','PUT',{status:0});
const summary={total:results.length,passed:results.filter(r=>r.pass).length,failed:results.filter(r=>!r.pass).length};fs.writeFileSync('docs/testing/review-results.json',JSON.stringify({time:new Date().toISOString(),tag,summary,results},null,2));console.log(JSON.stringify({summary,failures:results.filter(r=>!r.pass)},null,2));if(summary.failed)process.exitCode=1;
}
