import {base,testPassword,testUsername} from './test-env.mjs';
import fs from 'node:fs';
import path from 'node:path';
import net from 'node:net';
import {spawn,spawnSync} from 'node:child_process';
import {createHash} from 'node:crypto';
const config=JSON.parse(fs.readFileSync('backend/target/test-session.json','utf8')),tag='CLUSTER_'+Date.now(),checks=[],phases=[];let root='',child,activityId,super2,super3;
function check(name,pass,evidence){checks.push({name,pass:!!pass,evidence})}
function sql(statement){const r=spawnSync(config.MYSQL_BIN,['--host=127.0.0.1','--default-character-set=utf8mb4','--user='+config.TEST_DB_USER,'--database='+config.TEST_SCHEMA,'--batch','--skip-column-names','-e',statement],{env:{...process.env,MYSQL_PWD:config.TEST_DB_PASSWORD},encoding:'utf8'});if(r.status!==0)throw Error(r.stderr);return r.stdout.trim()}
async function req(server,route,method='GET',body,token=root){const r=await fetch(server+route,{method,headers:{...(token?{Authorization:'Bearer '+token}:{}),...(body===undefined?{}:{'Content-Type':'application/json'})},body:body===undefined?undefined:JSON.stringify(body),signal:AbortSignal.timeout(15000)});return{http:r.status,...await r.json()}}
async function login(server,username=testUsername,password=testPassword){return(await req(server,'/auth/login','POST',{username,password},'')).data.token}
const time=h=>new Date(Date.now()+(8+h)*3600000).toISOString().slice(0,19);
async function phase(name,count,concurrency,action,durationMs=0){let issued=0,completed=0,inflight=0,maxInflight=0,success=0;const timings=[],failures=[];const started=performance.now();
 async function worker(){while(true){if(durationMs&&performance.now()-started>=durationMs)return;const index=issued++;if(!durationMs&&index>=count)return;inflight++;maxInflight=Math.max(maxInflight,inflight);const t=performance.now();try{const r=await action(index);if(r.http===200&&r.code===0)success++;else failures.push({index,http:r.http,code:r.code,message:r.message})}catch(e){failures.push({index,error:e.message})}finally{timings.push(performance.now()-t);inflight--;completed++}}}
 await Promise.all(Array.from({length:concurrency},worker));const elapsedMs=performance.now()-started;if(durationMs)count=completed;timings.sort((a,b)=>a-b);const percentile=q=>timings[Math.max(0,Math.ceil(timings.length*q)-1)];const result={name,count,completed,success,failed:count-success,elapsedMs,rps:count/(elapsedMs/1000),latencyDenominator:'all completed requests, including failures',p50:percentile(.5),p95:percentile(.95),p99:percentile(.99),maxInflight,remainingInflight:inflight,failures};phases.push(result);check(name+'完备记账与成功率',completed===count&&success===count&&inflight===0,{success,count});console.log(JSON.stringify({phase:name,count,success,p95:result.p95,rps:result.rps}));return result}
try{
 root=await login(base);
 const server=net.createServer();await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));const port=server.address().port;await new Promise(resolve=>server.close(resolve));const second='http://127.0.0.1:'+port;
 const jar=config.jarPath||path.resolve('backend/target/activity-system-1.0.0.jar');
 const env={...process.env,CAMPUS_DB_URL:`jdbc:mysql://127.0.0.1:3306/${config.TEST_SCHEMA}?characterEncoding=UTF-8&serverTimezone=Asia/Shanghai`,CAMPUS_DB_USER:config.TEST_DB_USER,CAMPUS_DB_PASSWORD:config.TEST_DB_PASSWORD,CAMPUS_TESTING_ENABLED:'true'};
 const stdout=fs.openSync(path.join(process.env.TEST_OUTPUT_DIR,'second-instance.log'),'a');
 child=spawn('java',['-Xmx256m','-jar',jar,'--server.port='+port,'--campus.file.upload-dir=./target/isolated-uploads/'+config.TEST_SCHEMA],{cwd:path.resolve('backend'),env,windowsHide:true,stdio:['ignore',stdout,stdout]});
 let ready=false;for(let i=0;i<60;i++){try{const state=await req(second,'/testing/identity','GET',undefined,'');if(state.data?.schema===config.TEST_SCHEMA){ready=true;break}}catch{}await new Promise(resolve=>setTimeout(resolve,500))}if(!ready)throw Error('Second instance did not become ready');
 check('第二实例连接同一隔离库',ready);
 const password='Cluster123456';
 for(const suffix of ['S2','S3']){await req(base,'/super/admin/accounts','POST',{username:tag+suffix,realName:'并发超管验收',roleCode:'SUPER',passwordHash:password});}
 super2=Number(sql(`SELECT admin_id FROM t_admin WHERE username='${tag}S2'`));super3=Number(sql(`SELECT admin_id FROM t_admin WHERE username='${tag}S3'`));
 const token2=await login(base,tag+'S2',password),token3=await login(second,tag+'S3',password);
 await req(base,'/super/admin/accounts/1/status','PUT',{status:0},token2);
 const decisions=await Promise.all([req(base,`/super/admin/accounts/${super2}/status`,'PUT',{status:0},token2),req(second,`/super/admin/accounts/${super3}/status`,'PUT',{status:0},token3)]);
 check('双实例同时停用最后两名超管仅一次成功',decisions.filter(r=>r.code===0).length===1,{codes:decisions.map(r=>r.code)});
 check('双实例竞争后至少保留一名超管',Number(sql("SELECT COUNT(*) FROM t_admin WHERE role_code='SUPER' AND status=1"))===1);
 const surviving=Number(sql(`SELECT admin_id FROM t_admin WHERE admin_id IN (${super2},${super3}) AND status=1`));
 const restoreToken=await login(base,tag+(surviving===super2?'S2':'S3'),password);await req(base,'/super/admin/accounts/1/status','PUT',{status:1},restoreToken);root=await login(base);
 await req(base,`/super/admin/accounts/${super2}/status`,'PUT',{status:0});await req(base,`/super/admin/accounts/${super3}/status`,'PUT',{status:0});
 const created=await req(base,'/admin/activity','POST',{title:tag,typeCode:'LECTURE',location:'隔离压测场地',quota:500,signupDeadline:time(1),startTime:time(2),endTime:time(3)});activityId=created.data.activityId;await req(base,`/admin/activity/${activityId}/publish`,'POST');
 const guid=sql(`SELECT signup_guid FROM t_activity WHERE activity_id=${activityId}`),college=sql('SELECT college_code FROM t_college LIMIT 1');
 const count=300,phones=Array.from({length:count},(_,i)=>'138'+String(i).padStart(8,'0'));
 sql('INSERT INTO t_registration(activity_id,name,phone,college_code,status) VALUES '+phones.map(phone=>`(${activityId},'压测学生','${phone}','${college}','VALID')`).join(','));
 sql(`UPDATE t_activity SET status='ONGOING',signed_count=${count},start_time=DATE_SUB(NOW(),INTERVAL 1 HOUR),signup_deadline=DATE_SUB(NOW(),INTERVAL 2 HOUR),end_time=DATE_ADD(NOW(),INTERVAL 1 HOUR),checkin_enabled=1 WHERE activity_id=${activityId}`);
 for(let i=0;i<10;i++)await req(base,'/promo/'+guid,'GET',undefined,'');
 await phase('single-read',300,16,()=>req(base,'/promo/'+guid,'GET',undefined,''));
 await phase('dual-read',600,32,index=>req(index%2?second:base,'/promo/'+guid,'GET',undefined,''));
 await phase('single-read-15s',0,16,()=>req(base,'/promo/'+guid,'GET',undefined,''),15000);
 await phase('dual-read-15s',0,32,index=>req(index%2?second:base,'/promo/'+guid,'GET',undefined,''),15000);
 await phase('single-checkin',count,32,index=>req(base,'/checkin','POST',{activityId,phone:phones[index]},''));
 check('单实例签到写入行数完整',Number(sql(`SELECT COUNT(*) FROM t_checkin WHERE activity_id=${activityId}`))===count);
 sql(`DELETE FROM t_checkin WHERE activity_id=${activityId}`);
 await phase('dual-checkin',count,64,index=>req(index%2?second:base,'/checkin','POST',{activityId,phone:phones[index]},''));
 check('双实例签到写入行数完整',Number(sql(`SELECT COUNT(*) FROM t_checkin WHERE activity_id=${activityId}`))===count);
}catch(e){check('完整执行集群诊断',false,{error:e.stack})}finally{
 try{sql("UPDATE t_admin SET status=1,token_version=token_version+1 WHERE admin_id=1");root=await login(base);if(activityId)await req(base,'/admin/activity/'+activityId,'DELETE');for(const id of [super2,super3].filter(Boolean))await req(base,'/super/admin/accounts/'+id+'/status','PUT',{status:0});}catch(e){check('恢复测试管理员和关闭测试活动',false,e.message)}
 if(child)child.kill('SIGTERM');
 const jar=config.jarPath||'backend/target/activity-system-1.0.0.jar';const hash=file=>createHash('sha256').update(fs.readFileSync(file)).digest('hex');
 const summary={total:checks.length,passed:checks.filter(r=>r.pass).length,failed:checks.filter(r=>!r.pass).length};fs.writeFileSync('docs/testing/cluster-results.json',JSON.stringify({time:new Date().toISOString(),schema:config.TEST_SCHEMA,kind:'local bounded diagnostic, not production capacity acceptance',sourceJarHash:hash(jar),runnerHash:hash('docs/testing/cluster.mjs'),summary,checks,phases},null,2));console.log(JSON.stringify({summary,failures:checks.filter(r=>!r.pass)},null,2));if(summary.failed)process.exitCode=1;
}
