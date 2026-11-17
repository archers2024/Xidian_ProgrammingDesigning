import {reactive} from 'vue'
import request from './request'
export const session=reactive({profile:null,ready:false})
let loading
localStorage.removeItem('token')
export function establish(profile){session.profile=profile;session.ready=true}
export function clearSession(){session.profile=null;session.ready=false;loading=null}
export async function ensureSession(){
 if(session.ready)return session.profile
 if(!loading)loading=request.get('/admin/current',{silent:true}).then(profile=>{establish(profile);return profile}).catch(()=>{clearSession();return null}).finally(()=>{loading=null})
 return loading
}
window.addEventListener('session-expired',clearSession)
