import axios from 'axios'
import { clearAuth, getAuth } from '../utils/storage'

// API 客户端：统一注入 token，并处理 401
const client = axios.create({
  baseURL: '',
  timeout: 10000,
})

client.interceptors.request.use((config) => {
  const { token } = getAuth()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
    config.headers['X-Auth-Token'] = token
  }
  return config
})

client.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error?.response?.status
    if (status === 401 || (status === 403 && !error?.response?.data?.message)) {
      // token 失效时清理并跳转登录
      clearAuth()
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

export default client
