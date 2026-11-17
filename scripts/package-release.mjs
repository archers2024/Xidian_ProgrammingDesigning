import fs from 'node:fs';
import path from 'node:path';
import {createHash} from 'node:crypto';
const destination=path.resolve('backend/target/release-'+Date.now());
fs.mkdirSync(path.join(destination,'app'),{recursive:true});
fs.copyFileSync('backend/target/activity-system-1.0.0.jar',path.join(destination,'app/activity-system-1.0.0.jar'));
fs.cpSync('frontend/dist',path.join(destination,'web'),{recursive:true});fs.cpSync('deploy',path.join(destination,'deploy'),{recursive:true});fs.cpSync('db',path.join(destination,'db'),{recursive:true});
const records=[];
function walk(folder){for(const name of fs.readdirSync(folder)){const file=path.join(folder,name);if(fs.statSync(file).isDirectory())walk(file);else records.push(createHash('sha256').update(fs.readFileSync(file)).digest('hex')+'  '+path.relative(destination,file).split(path.sep).join('/'))}}
walk(destination);fs.writeFileSync(path.join(destination,'SHA256SUMS'),records.sort().join(String.fromCharCode(10))+String.fromCharCode(10));
console.log('Reviewable release package: '+destination);
