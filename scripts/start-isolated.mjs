import fs from 'node:fs';
import {spawn,spawnSync} from 'node:child_process';
import path from 'node:path';
import net from 'node:net';
const probe=net.createServer();await new Promise(resolve=>probe.listen(0,'127.0.0.1',resolve));const port=probe.address().port;await new Promise(resolve=>probe.close(resolve));
const schema='campus_test_'+Date.now();
const mysql=process.env.MYSQL_BIN||(process.platform==='win32'?'E:/mysql8.0/mysql-8.0.46-winx64/bin/mysql.exe':'mysql');
const password=process.env.TEST_DB_PASSWORD||fs.readFileSync('backend/config/local.properties','utf8').match(/^spring.datasource.password=(.*)$/m)[1];
const originalJar=path.resolve('backend/target/activity-system-1.0.0.jar');
const snapshotDirectory=path.resolve('backend/target/test-artifacts/'+schema);fs.mkdirSync(snapshotDirectory,{recursive:true});const jar=path.join(snapshotDirectory,'test-app.jar');fs.copyFileSync(originalJar,jar);
if(!/^campus_test_\d+$/.test(schema))throw Error('Unsafe schema');
function command(binary,args,env=process.env){const result=spawnSync(binary,args,{env,stdio:'inherit'});if(result.status!==0)throw Error(binary+' failed')}
const setupSql=`CREATE DATABASE ${schema} CHARACTER SET utf8mb4; CREATE USER IF NOT EXISTS 'campus_app'@'127.0.0.1' IDENTIFIED BY '${password.replaceAll("\\","\\\\").replaceAll("'","''")}'; GRANT SELECT,INSERT,UPDATE,DELETE ON ${schema}.* TO 'campus_app'@'127.0.0.1';`;
const prepared=spawnSync(mysql,['--host=127.0.0.1','--user=root'],{env:{...process.env,MYSQL_PWD:process.env.TEST_ROOT_PASSWORD||''},input:setupSql,stdio:['pipe','inherit','inherit']});if(prepared.status!==0)throw Error('Cannot prepare test schema');
const url=`jdbc:mysql://127.0.0.1:3306/${schema}?characterEncoding=UTF-8&serverTimezone=Asia/Shanghai`;
command('java',['-Dloader.main=com.campus.activity.tools.MigrationTool','-cp',jar,'org.springframework.boot.loader.launch.PropertiesLauncher'],{...process.env,CAMPUS_MIGRATION_URL:url,CAMPUS_MIGRATION_USER:'root',CAMPUS_MIGRATION_PASSWORD:process.env.TEST_ROOT_PASSWORD||''});
const initial='B7a'+crypto.randomUUID(),finalPassword='V8b'+crypto.randomUUID(),identitySecret=crypto.randomUUID()+crypto.randomUUID();
const env={...process.env,CAMPUS_CORS_ORIGINS:'http://localhost:5173,http://127.0.0.1:5173,http://127.0.0.1:4173',CAMPUS_DB_URL:url,CAMPUS_DB_USER:'campus_app',CAMPUS_DB_PASSWORD:password,CAMPUS_TESTING_ENABLED:'true',CAMPUS_BOOTSTRAP_PASSWORD:initial,CAMPUS_IDENTITY_SECRET:identitySecret,CAMPUS_IDENTITY_ISSUER:'urn:campus:test-idp',CAMPUS_IDENTITY_AUTHORIZE_URL:'https://identity.test.invalid/authorize'};
const stdout=fs.openSync(`backend/target/${schema}-run.log`,'a'),stderr=fs.openSync(`backend/target/${schema}-error.log`,'a');
const child=spawn('java',['-Xmx256m','-jar',jar,`--server.port=${port}`,`--campus.file.upload-dir=./target/isolated-uploads/${schema}`,'--campus.file.quota-bytes=4194304','--campus.signup.ip-limit-per-minute=10'],{cwd:path.resolve('backend'),env,detached:true,windowsHide:true,stdio:['ignore',stdout,stderr]});
child.unref();
const base=`http://127.0.0.1:${port}`;
let ready=false;
for(let i=0;i<50;i++){try{const health=await(await fetch(base+'/actuator/health/readiness')).json();if(health.status==='UP'){ready=true;break}}catch{}await new Promise(resolve=>setTimeout(resolve,500))}
if(!ready)throw Error(`Test server ${child.pid} did not become ready; inspect target/${schema}-run.log`);
async function post(route,method,body,token){return(await fetch(base+route,{method,headers:{'Content-Type':'application/json',...(token?{Authorization:'Bearer '+token}:{})},body:JSON.stringify(body)})).json()}
const login=await post('/auth/login','POST',{username:'admin',password:initial});
const changed=await post('/admin/password','PUT',{oldPassword:initial,newPassword:finalPassword},login.data.token);
if(changed.code!==0)throw Error('Could not activate test administrator');
const config={TEST_BASE:base,TEST_SCHEMA:schema,TEST_ADMIN_PASSWORD:finalPassword,TEST_ADMIN_USERNAME:'admin',TEST_ALLOW_MUTATIONS:'1',TEST_DB_USER:'campus_app',TEST_DB_PASSWORD:password,MYSQL_BIN:mysql,TEST_IDENTITY_SECRET:identitySecret,TEST_IDENTITY_ISSUER:'urn:campus:test-idp',jarPath:jar,pid:child.pid};
fs.writeFileSync('backend/target/test-session.json',JSON.stringify(config,null,2),{mode:0o600});
console.log(`Isolated server ready: ${schema}, port=${port}, pid=${child.pid}. Credentials are in ignored target/test-session.json.`);
