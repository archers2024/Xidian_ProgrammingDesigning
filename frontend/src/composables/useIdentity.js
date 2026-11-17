import {computed,ref,watch} from 'vue'
import request from '../api/request'
export function useIdentity(policy){
 const needed=computed(()=>['STRICT','IDENTITY_REQUIRED'].includes(policy())),enabled=ref(false),message=ref(''),hasProof=ref(false);let loaded=false
 const fragment=new URLSearchParams(location.hash.slice(1)),proof=fragment.get('identity_proof'),state=fragment.get('state')
 if(proof){if(proof.length<=8192&&state&&state===sessionStorage.getItem('campusIdentityState')){sessionStorage.setItem('campusIdentityProof',proof);sessionStorage.removeItem('campusIdentityState')}else message.value='身份验证返回信息无效，请重新验证';history.replaceState(null,'',location.pathname+location.search)}
 hasProof.value=!!sessionStorage.getItem('campusIdentityProof')
 let authorizeUrl=''
 watch(needed,async value=>{if(!value||loaded)return;loaded=true;try{const config=await request.get('/identity/config');enabled.value=config.enabled;authorizeUrl=config.authorizeUrl;if(!enabled.value)message.value='活动需要校园身份验证，请联系组织者启用认证入口'}catch(e){message.value=e.message}},{immediate:true})
 function start(){if(!enabled.value)return;const url=new URL(authorizeUrl);if(url.protocol!=='https:')return;const state=crypto.randomUUID();sessionStorage.setItem('campusIdentityState',state);url.searchParams.set('state',state);url.searchParams.set('return_to',location.origin+location.pathname+location.search);location.assign(url.href)}
 return{needed,enabled,message,hasProof,start}
}
