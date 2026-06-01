import axios from 'axios'
import { message } from 'antd'
import { getToken, removeToken } from '../utils/auth'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000,
})

request.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const { code, msg, data } = response.data
    if (code === 200) {
      return data
    }
    message.error(msg || '请求失败')
    return Promise.reject(new Error(msg))
  },
  (error) => {
    if (error.response?.status === 401) {
      removeToken()
      window.location.href = '/login'
    } else {
      message.error(error.response?.data?.msg || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default request
