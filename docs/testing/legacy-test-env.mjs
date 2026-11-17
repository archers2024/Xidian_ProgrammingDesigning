export {base,testPassword,testUsername} from './test-env.mjs';
const original=globalThis.fetch;
globalThis.fetch=async(input,config)=>{
 if(config?.method==='PUT'&&/\/admin\/activity\/\d+$/.test(String(input))&&typeof config.body==='string'){
  const body=JSON.parse(config.body);
  if(body.version==null){const detail=await original(input,{headers:config.headers});const json=await detail.json();if(json.code===0){body.version=json.data.activity.version;config={...config,body:JSON.stringify(body)}}}
 }
 return original(input,config)
};
