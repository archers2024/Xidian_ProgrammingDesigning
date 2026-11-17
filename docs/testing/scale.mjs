import './test-env.mjs';
import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import {spawnSync} from 'node:child_process';
const config=JSON.parse(fs.readFileSync('backend/target/test-session.json','utf8')),schema='campus_scale_'+Date.now(),checks=[],metrics={};
if(!/^campus_scale_\d+$/.test(schema)||!/^campus_test_\d+$/.test(config.TEST_SCHEMA))throw Error('Unsafe scope');
if(os.freemem()<2*1024**3)throw Error('Scale diagnostic requires 2GiB free RAM');
function sql(statement,root=false,db=schema){const r=spawnSync(config.MYSQL_BIN,['--host=127.0.0.1','--default-character-set=utf8mb4','--user='+(root?'root':config.TEST_DB_USER),'--database='+db,'--batch','--skip-column-names'],{env:{...process.env,MYSQL_PWD:root?(process.env.TEST_ROOT_PASSWORD||''):config.TEST_DB_PASSWORD},input:statement,encoding:'utf8',maxBuffer:16*1024*1024});if(r.status!==0)throw Error(r.stderr);return r.stdout.trim()}
const check=(name,pass)=>checks.push({name,pass:!!pass});
function explain(statement){return JSON.parse(sql('EXPLAIN FORMAT=JSON '+statement).replaceAll('\\n','\n'))}
function tablePlan(node,name){if(!node||typeof node!=='object')return null;if(node.table_name===name)return node;for(const value of Object.values(node)){if(Array.isArray(value)){for(const item of value){const found=tablePlan(item,name);if(found)return found}}else{const found=tablePlan(value,name);if(found)return found}}return null}
try{
 const dataDir=sql("SELECT @@datadir",false,config.TEST_SCHEMA);const disk=fs.statfsSync(dataDir);const free=Number(disk.bavail)*Number(disk.bsize);if(free<5*1024**3)throw Error('Scale diagnostic requires 5GiB free database disk');metrics.freeDiskBefore=free;
 sql(`CREATE DATABASE ${schema} CHARACTER SET utf8mb4;CREATE TABLE ${schema}.t_activity LIKE ${config.TEST_SCHEMA}.t_activity;CREATE TABLE ${schema}.t_registration LIKE ${config.TEST_SCHEMA}.t_registration;CREATE TABLE ${schema}.t_checkin LIKE ${config.TEST_SCHEMA}.t_checkin;GRANT SELECT,INSERT,UPDATE,DELETE ON ${schema}.* TO 'campus_app'@'127.0.0.1';`,true,config.TEST_SCHEMA);
 const started=performance.now(),batch=10000,total=1000000;
 for(let offset=0;offset<total;offset+=batch){
  sql(`SET SESSION cte_max_recursion_depth=10000;INSERT INTO t_activity(org_id,activity_no,signup_guid,title,type_code,location,start_time,end_time,signup_deadline,quota,signed_count,checkin_enabled,checkin_secret,status,created_by,deleted,version,checkin_policy) WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n+1 FROM seq WHERE n<${batch}) SELECT 1,CONCAT('S',n+${offset}),CONCAT('G',n+${offset}),'隔离历史规模样本','LECTURE','规模测试场地',DATE_SUB(NOW(),INTERVAL 2 HOUR),DATE_SUB(NOW(),INTERVAL 1 HOUR),DATE_SUB(NOW(),INTERVAL 3 HOUR),5000,0,0,'scale-fixture-only','FINISHED',1,0,0,'BASIC' FROM seq;`);
  if((offset+batch)%100000===0){fs.writeFileSync('docs/testing/scale-progress.json',JSON.stringify({schema,inserted:offset+batch,total,elapsedMs:performance.now()-started}));console.log('Seeded '+(offset+batch)+' historical rows')}
 }
 metrics.seedElapsedMs=performance.now()-started;metrics.historicalRows=Number(sql('SELECT COUNT(*) FROM t_activity'));check('已建立一百万历史活动隔离数据集',metrics.historicalRows===total);
 sql('ANALYZE TABLE t_activity');
 const source=fs.readFileSync('backend/src/main/java/com/campus/activity/module/activity/mapper/ActivityMapper.java','utf8'),section=source.slice(source.indexOf('@Update("UPDATE t_activity SET status'),source.indexOf('int refreshStatuses'));
 const update=[...section.matchAll(/"([^"]*)"/g)].map(m=>m[1]).join(''),previous=update.replace("status IN ('OPEN','CLOSED','ONGOING')","status IN ('OPEN','CLOSED','ONGOING','FINISHED')");
 metrics.currentPlan=explain(update);metrics.previousPredicatePlan=explain(previous);metrics.currentCandidates=Number(sql("SELECT COUNT(*) FROM t_activity WHERE deleted=0 AND status IN ('OPEN','CLOSED','ONGOING')"));metrics.previousCandidates=Number(sql("SELECT COUNT(*) FROM t_activity WHERE deleted=0 AND status IN ('OPEN','CLOSED','ONGOING','FINISHED')"));
 check('结束活动不再进入状态推进候选集',metrics.currentCandidates===0&&metrics.previousCandidates===total);
 const refreshed=performance.now();sql(update);metrics.currentRefreshMs=performance.now()-refreshed;
 sql("SET SESSION cte_max_recursion_depth=5000;INSERT INTO t_registration(activity_id,name,phone,college_code,status) WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n+1 FROM seq WHERE n<5000) SELECT 1,'索引诊断样本',CONCAT('139',LPAD(n,8,'0')),'FIXTURE','VALID' FROM seq;INSERT INTO t_checkin(reg_id,activity_id,checkin_time,method,device_fingerprint) SELECT reg_id,1,DATE_SUB(NOW(),INTERVAL (reg_id%120) SECOND),'ACCURATE',LPAD(CONV(reg_id%100,10,16),16,'0') FROM t_registration;UPDATE t_activity SET signed_count=5000 WHERE activity_id=1;ANALYZE TABLE t_checkin;");
 const query="SELECT COUNT(*) FROM t_checkin WHERE activity_id=1 AND device_fingerprint='0000000000000000' AND checkin_time>=DATE_SUB(NOW(),INTERVAL 300 SECOND)";
 metrics.devicePlan=explain(query);metrics.deviceRows=Number(sql(query));metrics.selectedDeviceIndex=tablePlan(metrics.devicePlan,'t_checkin')?.key;
 check('设备限频查询选择联合索引',metrics.selectedDeviceIndex==='idx_checkin_device_window');check('设备与时间过滤命中预期50条',metrics.deviceRows===50);
 metrics.tableBytes=Number(sql("SELECT SUM(data_length+index_length) FROM information_schema.tables WHERE table_schema=DATABASE()"));
}catch(e){checks.push({name:'完整执行规模与执行计划诊断',pass:false,evidence:e.stack})}finally{
 const summary={total:checks.length,passed:checks.filter(x=>x.pass).length,failed:checks.filter(x=>!x.pass).length};fs.writeFileSync('docs/testing/scale-results.json',JSON.stringify({time:new Date().toISOString(),schema,kind:'isolated MySQL query-plan diagnostic; not an end-to-end production SLA',summary,checks,metrics},null,2));console.log(JSON.stringify({summary,failures:checks.filter(x=>!x.pass),schema,historicalRows:metrics.historicalRows,currentCandidates:metrics.currentCandidates,selectedDeviceIndex:metrics.selectedDeviceIndex},null,2));if(summary.failed)process.exitCode=1;
}
