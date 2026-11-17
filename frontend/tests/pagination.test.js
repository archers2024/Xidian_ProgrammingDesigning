import {describe,it,expect,vi} from 'vitest'
import {createApp} from 'vue'
import {usePagedResource} from '../src/composables/usePagedResource'
function mounted(loader){let state;const app=createApp({setup(){state=usePagedResource(loader);return()=>null}});app.mount(document.createElement('div'));return{state,close:()=>app.unmount()}}
describe('paged requests',()=>{
 it('ignores older responses and loading states',async()=>{const resolvers=[];const fixture=mounted(()=>new Promise(resolve=>resolvers.push(resolve)));const a=fixture.state.load(),b=fixture.state.load();resolvers[1]({records:['new'],total:1});await b;resolvers[0]({records:['old'],total:9});await a;expect(fixture.state.rows.value).toEqual(['new']);expect(fixture.state.total.value).toBe(1);fixture.close()})
 it('separates errors from successful empty pages',async()=>{const fixture=mounted(vi.fn().mockRejectedValueOnce(Error('offline')).mockResolvedValue({records:[],total:0}));await fixture.state.load();expect(fixture.state.error.value).toBe('offline');await fixture.state.load();expect(fixture.state.error.value).toBe('');expect(fixture.state.rows.value).toEqual([]);fixture.close()})
 it('does not update an unmounted view',async()=>{let resolve;const fixture=mounted(()=>new Promise(done=>{resolve=done}));const pending=fixture.state.load();fixture.close();resolve({records:['late'],total:1});await pending;expect(fixture.state.rows.value).toEqual([])})
})
