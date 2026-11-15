import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api/v1',
  timeout: 15000
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = 'Bearer ' + token
  }
  return config
})

request.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body && body.code === 0) {
      return body.data
    }
    ElMessage.error((body && body.message) || '请求失败')
    return Promise.reject(new Error(body?.message || '请求失败'))
  },
  (err) => {
    if (err.response && err.response.status === 401) {
      localStorage.removeItem('token')
      window.location.href = '/login'
    }
    ElMessage.error('网络异常，请稍后重试')
    return Promise.reject(err)
  }
)

export default request
