import {base as testBase,testPassword,testUsername} from './legacy-test-env.mjs';
import fs from 'node:fs';
const base=testBase,tag='STRUCT_'+Date.now(),results=[],ids=[];let token='',org;
async function req(path,method='GET',body){const r=await fetch(base+path,{method,headers:{Authorization:'Bearer '+token,...(body===undefined?{}:{'Content-Type':'application/json'})},body:body===undefined?undefined:JSON.stringify(body)});return{http:r.status,...await r.json()}}
function check(name,pass,evidence){results.push({name,pass:!!pass,evidence})}
const date=h=>new Date(Date.now()+(8+h)*3600000).toISOString().slice(0,19);
try{
 token=(await req('/auth/login','POST',{username:testUsername,password:testPassword})).data.token;
 const created=await req('/super/organizations','POST',{code:tag,name:'结构化回归测试',requireReview:false,status:0,orgId:1});org=created.data.orgId;
 check('新组织输入不能绕过默认审批策略',created.data.requireReview===true&&created.data.status===1&&org!==1);
 const body={orgId:org,title:tag,typeCode:'LECTURE',location:'测试场地',quota:10,signupDeadline:date(1),startTime:date(2),endTime:date(3),attachments:[{fileName:'活动说明.pdf',url:'/api/v1/files/contract-test.pdf',fileSize:32}]};
 const activity=await req('/admin/activity','POST',{...body,status:'OPEN',approvalStatus:'APPROVED',signedCount:999,createdBy:999});const id=activity.data.activityId;ids.push(id);const path='/admin/activity/'+id;
 let detail=(await req(path)).data;
 check('不可批量注入活动状态和报名计数',detail.activity.status==='DRAFT'&&detail.activity.approvalStatus==='NONE'&&detail.activity.signedCount===0);
 check('附件结构化请求可持久化',detail.attachments.length===1&&detail.attachments[0].fileName==='活动说明.pdf');
 const {attachments,...withoutAttachments}=body;
 check('省略附件时更新成功',(await req(path,'PUT',{...withoutAttachments,title:tag+'改稿'})).code===0);
 check('省略附件保持原附件',(await req(path)).data.attachments.length===1);
 check('空附件数组明确清空附件',(await req(path,'PUT',{...body,attachments:[]})).code===0&&(await req(path)).data.attachments.length===0);
 for(const bad of [{quota:2.5},{orgId:org+0.5},{title:42},{attachments:[null]},{attachments:[{fileName:2,url:'/x.pdf'}]},{attachments:[{fileName:'x',url:'/x.pdf',fileSize:1.2}]}]){
  const r=await req('/admin/activity','POST',{...body,...bad});check('严格输入边界 '+JSON.stringify(bad),r.code===40001,{code:r.code,message:r.message});
 }
 check('审批枚举拒绝未知值',(await req('/super/reviews/1/decision','PUT',{decision:'NOT_A_DECISION'})).code===40001);
}catch(e){check('完整执行',false,e.stack)}finally{
 for(const id of ids)await req('/admin/activity/'+id,'DELETE');if(org)await req('/super/organizations/'+org+'/status','PUT',{status:0});
 const summary={total:results.length,passed:results.filter(r=>r.pass).length,failed:results.filter(r=>!r.pass).length};fs.writeFileSync('docs/testing/structure-results.json',JSON.stringify({time:new Date().toISOString(),tag,summary,results},null,2));console.log(JSON.stringify({summary,failures:results.filter(r=>!r.pass)},null,2));if(summary.failed)process.exitCode=1;
}
