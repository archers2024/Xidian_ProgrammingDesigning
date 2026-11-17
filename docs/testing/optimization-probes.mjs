import fs from 'node:fs';
const base='http://127.0.0.1:8080',results=[];
for(const path of ['/dict/activity-type','/admin/current','/actuator/health','/actuator/prometheus','/__audit_missing_route_20261116']){
 try{const r=await fetch(base+path);const body=await r.json().catch(()=>null);results.push({path,status:r.status,bodyCode:body?.code,message:body?.message,headers:Object.fromEntries(['cache-control','content-security-policy','referrer-policy','x-frame-options','x-content-type-options'].map(k=>[k,r.headers.get(k)]))})}catch(e){results.push({path,error:e.message})}
}
try{const r=await fetch(base+'/admin/current',{method:'OPTIONS',headers:{Origin:'https://audit.invalid','Access-Control-Request-Method':'GET','Access-Control-Request-Headers':'authorization'}});results.push({path:'/admin/current',method:'OPTIONS',origin:'https://audit.invalid',status:r.status,allowOrigin:r.headers.get('access-control-allow-origin'),allowCredentials:r.headers.get('access-control-allow-credentials')})}catch(e){results.push({error:e.message})}
fs.writeFileSync(process.env.PROBE_OUTPUT || ('docs/testing/optimization-probes-'+Date.now()+'.json'),JSON.stringify({time:new Date().toISOString(),readOnly:true,results},null,2));console.log(JSON.stringify(results,null,2));
