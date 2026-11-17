import {spawn} from 'node:child_process';
const run=file=>new Promise(resolve=>{const child=spawn(process.execPath,[file],{stdio:'inherit'});child.on('exit',code=>resolve(code??1));child.on('error',()=>resolve(1));});
const first=await run('docs/testing/audit.mjs');
// Use separate minute windows so the real signup limiter is tested without one suite exhausting the next.
const cooldown=61000-Date.now()%60000;
console.log(`Waiting ${Math.ceil(cooldown/1000)}s for the next signup limit window`);
await new Promise(resolve=>setTimeout(resolve,cooldown));
const second=await run('docs/testing/round2.mjs');
process.exitCode=first||second;
