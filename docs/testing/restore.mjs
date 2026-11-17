import {base,testPassword,testUsername} from './test-env.mjs';
import fs from 'node:fs';
import path from 'node:path';
import net from 'node:net';
import {spawn,spawnSync} from 'node:child_process';
import {randomBytes,scryptSync,createCipheriv,createDecipheriv,createHash} from 'node:crypto';
const config=JSON.parse(fs.readFileSync('backend/target/test-session.json','utf8')),target='campus_restore_'+Date.now(),checks=[];let child;
if(!/^campus_restore_\d+$/.test(target))throw Error('Unsafe restore target');
const check=(name,pass)=>checks.push({name,pass:!!pass});
const hash=buffer=>createHash('sha256').update(buffer).digest('hex');
function mysql(schema,sql,root=false){const r=spawnSync(config.MYSQL_BIN,['--host=127.0.0.1','--default-character-set=utf8mb4','--user='+(root?'root':config.TEST_DB_USER),'--database='+schema,'--batch','--skip-column-names'],{env:{...process.env,MYSQL_PWD:root?(process.env.TEST_ROOT_PASSWORD||''):config.TEST_DB_PASSWORD},input:sql,encoding:'utf8',maxBuffer:64*1024*1024});if(r.status!==0)throw Error(r.stderr);return r.stdout.trim()}
const work=path.resolve('backend/target/restore-drill-'+Date.now()),uploadRoot=path.resolve('backend/target/isolated-uploads/'+config.TEST_SCHEMA);fs.mkdirSync(work,{recursive:true});
const tables=['t_organization','t_admin','t_activity','t_activity_review','t_registration','t_checkin','t_attachment','t_file_asset'];
function counts(schema){return Object.fromEntries(tables.map(table=>[table,Number(mysql(schema,'SELECT COUNT(*) FROM '+table))]))}
const files=[];function walk(folder){if(!fs.existsSync(folder))return;for(const entry of fs.readdirSync(folder)){const file=path.join(folder,entry),stat=fs.lstatSync(file);if(stat.isSymbolicLink())throw Error('Unexpected symbolic link in backup');if(stat.isDirectory())walk(file);else files.push({name:path.relative(uploadRoot,file).split(path.sep).join('/'),data:fs.readFileSync(file).toString('base64'),sha256:hash(fs.readFileSync(file))})}}
let metrics={};
try{
 const before=counts(config.TEST_SCHEMA);walk(uploadRoot);
 const dumpBinary=process.env.MYSQLDUMP_BIN||(process.platform==='win32'?path.join(path.dirname(config.MYSQL_BIN),'mysqldump.exe'):'mysqldump');
 const dump=spawnSync(dumpBinary,['--host=127.0.0.1','--user=root','--single-transaction','--no-tablespaces','--set-gtid-purged=OFF','--routines','--triggers',config.TEST_SCHEMA],{env:{...process.env,MYSQL_PWD:process.env.TEST_ROOT_PASSWORD||''},maxBuffer:64*1024*1024});if(dump.status!==0)throw Error(String(dump.stderr));
 check('备份不包含切换目标库语句',!/^\s*(USE|CREATE DATABASE)\s/im.test(dump.stdout.toString('utf8')));
 const bundle=Buffer.from(JSON.stringify({database:dump.stdout.toString('base64'),files}));
 const password=randomBytes(32),salt=randomBytes(16),iv=randomBytes(12),key=scryptSync(password,salt,32),cipher=createCipheriv('aes-256-gcm',key,iv);
 const ciphertext=Buffer.concat([cipher.update(bundle),cipher.final()]),tag=cipher.getAuthTag();
 fs.writeFileSync(path.join(work,'backup.enc'),Buffer.concat([salt,iv,tag,ciphertext]),{mode:0o600});
 const corrupt=Buffer.from(ciphertext);corrupt[Math.floor(corrupt.length/2)]^=1;let rejected=false;try{const bad=createDecipheriv('aes-256-gcm',key,iv);bad.setAuthTag(tag);bad.update(corrupt);bad.final()}catch{rejected=true}check('篡改的加密备份被拒绝',rejected);
 const restoreStart=performance.now();const decipher=createDecipheriv('aes-256-gcm',key,iv);decipher.setAuthTag(tag);const decrypted=Buffer.concat([decipher.update(ciphertext),decipher.final()]);const recovered=JSON.parse(decrypted.toString('utf8'));
 mysql(config.TEST_SCHEMA,`CREATE DATABASE ${target} CHARACTER SET utf8mb4;GRANT SELECT,INSERT,UPDATE,DELETE ON ${target}.* TO 'campus_app'@'127.0.0.1';`,true);
 mysql(target,Buffer.from(recovered.database,'base64'),true);
 const restoredRoot=path.join(work,'uploads');fs.mkdirSync(restoredRoot,{recursive:true});
 for(const file of recovered.files){const output=path.resolve(restoredRoot,file.name);if(!output.startsWith(restoredRoot+path.sep))throw Error('Unsafe archive path');fs.mkdirSync(path.dirname(output),{recursive:true});fs.writeFileSync(output,Buffer.from(file.data,'base64'));if(hash(fs.readFileSync(output))!==file.sha256)throw Error('File hash mismatch')}
 check('恢复后核心表记录总数一致',JSON.stringify(before)===JSON.stringify(counts(target)));check('恢复后上传文件逐个哈希一致',recovered.files.every(file=>hash(fs.readFileSync(path.join(restoredRoot,file.name)))===file.sha256));
 const probe=net.createServer();await new Promise(resolve=>probe.listen(0,'127.0.0.1',resolve));const port=probe.address().port;await new Promise(resolve=>probe.close(resolve));
 const out=fs.openSync(path.join(work,'restored-server.log'),'a');
 child=spawn('java',['-Xmx256m','-jar',config.jarPath,'--server.port='+port,'--campus.file.upload-dir='+restoredRoot],{cwd:path.resolve('backend'),env:{...process.env,CAMPUS_DB_URL:`jdbc:mysql://127.0.0.1:3306/${target}?characterEncoding=UTF-8&serverTimezone=Asia/Shanghai`,CAMPUS_DB_USER:config.TEST_DB_USER,CAMPUS_DB_PASSWORD:config.TEST_DB_PASSWORD},windowsHide:true,stdio:['ignore',out,out]});
 let ready=false;for(let i=0;i<60;i++){try{const r=await fetch(`http://127.0.0.1:${port}/actuator/health/readiness`);if((await r.json()).status==='UP'){ready=true;break}}catch{}await new Promise(resolve=>setTimeout(resolve,500))}check('恢复库可以启动应用并达到就绪',ready);
 if(ready){const r=await fetch(`http://127.0.0.1:${port}/auth/login`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({username:testUsername,password:testPassword})});check('恢复后的账号可以正常登录',(await r.json()).code===0)}
 metrics={source:config.TEST_SCHEMA,target,coreTableCounts:before,fileCount:files.length,encryptedBytes:ciphertext.length,encryptedSha256:hash(ciphertext),restoreElapsedMs:performance.now()-restoreStart};
 bundle.fill(0);decrypted.fill(0);password.fill(0);key.fill(0);dump.stdout.fill(0);
}catch(e){checks.push({name:'完整执行隔离恢复演练',pass:false,evidence:e.stack})}finally{
 if(child)child.kill('SIGTERM');const summary={total:checks.length,passed:checks.filter(x=>x.pass).length,failed:checks.filter(x=>!x.pass).length};fs.writeFileSync('docs/testing/restore-results.json',JSON.stringify({summary,checks,metrics,kind:'quiescent local isolated restore drill; not a production RPO/RTO guarantee',work},null,2));console.log(JSON.stringify({summary,failures:checks.filter(x=>!x.pass),metrics},null,2));if(summary.failed)process.exitCode=1;
}
