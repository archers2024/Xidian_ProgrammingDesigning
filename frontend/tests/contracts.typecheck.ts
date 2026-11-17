import {activityApi} from '../src/api/activities'
activityApi.detail(1).then(result=>{
 const title:string=result.activity.title
 const version:number=result.activity.version
 // @ts-expect-error contract rejects a misspelled response field
 result.actviity
 return {title,version}
})
// @ts-expect-error a mutation cannot omit its required fields
activityApi.create({title:'incomplete'})
