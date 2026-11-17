export const reviewApi = {
 snapshot: id => request.get(`/admin/reviews/${id}/snapshot`),
 queue: (params) => request.get('/admin/reviews',{params}),
 submit: (id) => request.post(`/admin/activity/${id}/submit-review`),
 history: (id) => request.get(`/admin/activity/${id}/reviews`),
 decide: (id,data) => request.put(`/super/reviews/${id}/decision`,data)
}
import request from './request'
export const organizationApi = {
  reviewPolicy: (id,requireReview) => request.put(`/super/organizations/${id}/review-policy`,{requireReview}),
  available: (keyword) => request.get('/admin/organizations',{params:{keyword}}),
  list: () => request.get('/super/organizations'),
  page: (params) => request.get('/super/organizations/page',{params}),
  create: (data) => request.post('/super/organizations', data),
  status: (id,status) => request.put(`/super/organizations/${id}/status`,{status}),
  audit: (params) => request.get('/super/audit-logs',{params})
}

// A岗 认证与账号
export const authApi = {
  login: (data) => request.post('/auth/login', data),
  logout: () => request.post('/auth/logout'),
  current: () => request.get('/admin/current'),
  changePassword: (data) => request.put('/admin/password', data)
}

export const adminApi = {
  list: () => request.get('/super/admin/accounts'),
  page: (params) => request.get('/super/admin/accounts/page',{params}),
  create: (data) => request.post('/super/admin/accounts', data),
  status: (id, status) => request.put(`/super/admin/accounts/${id}/status`, { status }),
  resetPwd: (id) => request.put(`/super/admin/accounts/${id}/password`)
}

export const fileApi = {
  upload: (formData) => request.post('/file/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// B岗 活动域
export {activityApi} from './activities'

// C岗 报名域
export const signupApi = {
  promo: (guid) => request.get(`/promo/${guid}`),
  submit: (data,key) => request.post('/signup',data,{headers:key?{'Idempotency-Key':key}:{}}),
  page: (id,params) => request.get(`/admin/activity/${id}/registrations/page`,{params}),
  list: (activityId, maskPhone = true) =>
    request.get(`/admin/activity/${activityId}/registrations`, { params: { maskPhone } }),
  // Cookie session and CSRF protection; export through authenticated Blob requests.
  exportUrl: (activityId, maskPhone = true) =>
    `/admin/activity/${activityId}/registrations/export?maskPhone=${maskPhone}`
}

// D岗 签到与统计域
export const checkinApi = {
  info:(id)=>request.get(`/checkin/activity/${id}`),
  board: (activityId) => request.get(`/checkin/qrcode/${activityId}`),
  submit: (data) => request.post('/checkin', data),
  page: (id,params) => request.get(`/admin/activity/${id}/checkins/page`,{params}),
  list: (activityId) => request.get(`/admin/activity/${activityId}/checkins`),
  exportUrl: (activityId) =>
    `/admin/activity/${activityId}/checkins/export`
}

export const statisticsApi = {
  overview: (days = 30) => request.get('/admin/statistics/overview', { params: { days } })
}

// 字典
export const dictApi = {
  activityTypes: () => request.get('/dict/activity-type'),
  colleges: () => request.get('/dict/college')
}
