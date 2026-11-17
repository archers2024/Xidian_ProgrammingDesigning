import fs from 'node:fs';
import {spawnSync} from 'node:child_process';
import path from 'node:path';
const config=JSON.parse(fs.readFileSync('backend/target/test-session.json','utf8'));
if(!/^campus_test_\d+$/.test(config.TEST_SCHEMA))throw Error('Unsafe target');
const directory=path.resolve('docs/testing/runs/run-'+Date.now());fs.mkdirSync(directory,{recursive:true});
const env={...process.env,...Object.fromEntries(Object.entries(config).filter(([key])=>key!=='pid')),TEST_OUTPUT_DIR:directory};
const suites=process.argv.slice(2).length?process.argv.slice(2):['audit','round2','platform','review','structure','round3','enterprise','topic2'];
const results=[];
for(const suite of suites){
 if(!/^[A-Za-z0-9_-]+$/.test(suite))throw Error('Invalid suite');
 const identity=await(await fetch(config.TEST_BASE+'/testing/identity')).json();
 if(!identity.data?.testing||identity.data.schema!==config.TEST_SCHEMA)throw Error('Target identity mismatch');
 const cleanup=spawnSync(config.MYSQL_BIN,['--host=127.0.0.1','--user='+config.TEST_DB_USER,'--database='+config.TEST_SCHEMA,'--execute=DELETE FROM t_rate_limit'],{env:{...env,MYSQL_PWD:config.TEST_DB_PASSWORD},encoding:'utf8'});
 if(cleanup.status!==0)throw Error(cleanup.stderr);
 const result=spawnSync(process.execPath,['docs/testing/'+suite+'.mjs'],{env,encoding:'utf8',maxBuffer:10*1024*1024});
 fs.writeFileSync(path.join(directory,suite+'-console.txt'),result.stdout+'\n'+result.stderr);
 results.push({suite,exitCode:result.status});console.log(suite+': '+result.status);
}
fs.writeFileSync(path.join(directory,'suite-exit-codes.json'),JSON.stringify(results,null,2));
console.log('Preserved results: '+directory);if(results.some(item=>item.exitCode!==0))process.exitCode=1;
