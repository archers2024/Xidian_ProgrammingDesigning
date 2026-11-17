import {ref,reactive,onBeforeUnmount} from 'vue'
/** Only the latest request may update the view, including its loading/error state. */
export function usePagedResource(fetchPage,defaults={}){
 const rows=ref([]),total=ref(0),loading=ref(false),error=ref(''),query=reactive({current:1,size:20,...defaults});let sequence=0
 async function load(){const current=++sequence;loading.value=true;error.value='';try{const data=await fetchPage({...query});if(current!==sequence)return;rows.value=data.records;total.value=data.total}catch(e){if(current===sequence){error.value=e.message;rows.value=[];total.value=0}}finally{if(current===sequence)loading.value=false}}
 function search(){query.current=1;return load()}
 function page(current){query.current=current;return load()}
 onBeforeUnmount(()=>{sequence++})
 return{rows,total,loading,error,query,load,search,page}
}
