import request from '../api/request'
export async function download(url,name){const blob=await request.get(url,{responseType:'blob',timeout:60000});const objectUrl=URL.createObjectURL(blob);const link=document.createElement('a');link.href=objectUrl;link.download=name;link.click();setTimeout(()=>URL.revokeObjectURL(objectUrl),1000)}
