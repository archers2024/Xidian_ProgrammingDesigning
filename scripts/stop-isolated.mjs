import fs from 'node:fs';
import {execFileSync} from 'node:child_process';
const config=JSON.parse(fs.readFileSync('backend/target/test-session.json','utf8'));
if(!Number.isInteger(config.pid)||!/^campus_test_\d+$/.test(config.TEST_SCHEMA))throw Error('Invalid process receipt');
let command='';try{command=process.platform==='win32'?execFileSync('powershell.exe',['-NoProfile','-Command',`(Get-CimInstance Win32_Process -Filter 'ProcessId=${config.pid}').CommandLine`],{encoding:'utf8'}):execFileSync('ps',['-p',String(config.pid),'-o','args='],{encoding:'utf8'})}catch{}
if(!command.trim()){console.log('Owned test process already exited');process.exit(0)}
if(!command.includes(config.TEST_SCHEMA)||!command.includes('-jar'))throw Error('Process ownership does not match; refusing to stop');
process.kill(config.pid,'SIGTERM');console.log('Stopped owned isolated process '+config.pid+'; database and evidence retained.');
