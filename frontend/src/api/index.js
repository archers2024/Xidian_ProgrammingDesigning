import request from './request'

// A岗 认证与账号
export const authApi = {
  login: (data) => request.post('/auth/login', data),
  logout: () => request.post('/auth/logout'),
  current: () => request.get('/admin/current')
}

export const adminApi = {
  list: () => request.get('/super/admin/accounts'),
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
export const activityApi = {
  page: (params) => request.get('/admin/activity/list', { params }),
  detail: (id) => request.get(`/admin/activity/${id}`),
  create: (data) => request.post('/admin/activity', data),
  update: (id, data) => request.put(`/admin/activity/${id}`, data),
  remove: (id) => request.delete(`/admin/activity/${id}`),
  publish: (id) => request.post(`/admin/activity/${id}/publish`),
  toggleCheckin: (id, enabled) => request.put(`/admin/activity/${id}/checkin`, { enabled }),
  qrcode: (id) => request.get(`/admin/activity/${id}/qrcode`)
}

// C岗 报名域
export const signupApi = {
  promo: (guid) => request.get(`/promo/${guid}`),
  submit: (data) => request.post('/signup', data),
  list: (activityId, maskPhone = true) =>
    request.get(`/admin/activity/${activityId}/registrations`, { params: { maskPhone } }),
  // 导出用浏览器直链（无法带 Header），token 走 query 参数，后端拦截器已支持
  exportUrl: (activityId, maskPhone = true) =>
    `/api/v1/admin/activity/${activityId}/registrations/export?maskPhone=${maskPhone}&token=${localStorage.getItem('token') || ''}`
}

// D岗 签到与统计域
export const checkinApi = {
  board: (activityId) => request.get(`/checkin/qrcode/${activityId}`),
  submit: (data) => request.post('/checkin', data),
  list: (activityId) => request.get(`/admin/activity/${activityId}/checkins`),
  exportUrl: (activityId) =>
    `/api/v1/admin/activity/${activityId}/checkins/export?token=${localStorage.getItem('token') || ''}`
}

export const statisticsApi = {
  overview: (days = 30) => request.get('/admin/statistics/overview', { params: { days } })
}

// 字典
export const dictApi = {
  activityTypes: () => request.get('/dict/activity-type'),
  colleges: () => request.get('/dict/college')
}
