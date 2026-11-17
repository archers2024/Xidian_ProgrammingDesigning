import axios from 'axios'
import {ElMessage} from 'element-plus'
const request=axios.create({baseURL:'/api/v1',timeout:15000,withCredentials:true})
request.interceptors.request.use(config=>{
 const csrf=document.cookie.split('; ').find(v=>v.startsWith('CAMPUS_CSRF='))?.split('=')[1]
 if(csrf&&!['get','head','options'].includes(config.method||'get'))config.headers['X-CSRF-Token']=decodeURIComponent(csrf)
 return config
})
function fail(message,code,status,config){const error=new Error(message);error.code=code;error.status=status;if(!config?.silent)ElMessage.error(message);return Promise.reject(error)}
request.interceptors.response.use(async res=>{
 let body=res.data
 if(res.config.responseType==='blob'){
  if(!String(res.headers['content-type']).includes('json'))return body
  body=JSON.parse(await body.text())
 }
 if(body?.code===0)return body.data
 return fail(body?.message||'请求失败',body?.code,res.status,res.config)
},err=>{
 if(err.code==='ERR_CANCELED')return Promise.reject(err)
 const status=err.response?.status
 const labels={401:'登录已过期，请重新登录',403:'没有访问权限',404:'内容不存在或已移除',409:'内容已更新，请刷新后重试',429:'操作频繁，请稍后重试',503:'服务暂不可用，请稍后重试'}
 const message=err.response?.data?.message||labels[status]||(err.code==='ECONNABORTED'?'请求超时，请重试':status>=500?'服务暂时出现问题，请稍后重试':'网络连接失败，请检查网络后重试')
 if(status===401)window.dispatchEvent(new Event('session-expired'))
 return fail(message,err.response?.data?.code,status,err.config)
})
export default request
