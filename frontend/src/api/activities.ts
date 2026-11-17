import request from './request'
import type {Activity,ActivitySave,ActivityDetail,Page} from './contracts'
export const activityApi={
 page:(params:Record<string,unknown>)=>request.get<Page<Activity>>('/admin/activity/list',{params}),
 detail:(id:number|string)=>request.get<ActivityDetail>(`/admin/activity/${id}`),
 create:(data:ActivitySave,key?:string)=>request.post<{activityId:number;signupGuid:string}>('/admin/activity',data,{headers:key?{'Idempotency-Key':key}:{}}),
 update:(id:number|string,data:ActivitySave)=>request.put<void>(`/admin/activity/${id}`,data),
 remove:(id:number|string)=>request.delete<void>(`/admin/activity/${id}`),
 publish:(id:number|string)=>request.post<void>(`/admin/activity/${id}/publish`),
 checkinEnabled:(id:number|string,enabled:0|1)=>request.put<void>(`/admin/activity/${id}/checkin-enabled`,{enabled}),
 summary:(id:number|string)=>request.get<{signedCount:number;checkinCount:number}>(`/admin/activity/${id}/attendance-summary`),
 qrcode:(id:number|string)=>request.get<{guid:string;title:string}>(`/admin/activity/${id}/qrcode`)
}
