import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useUserStore } from '@/stores/user'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

request.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  (response) => {
    return response.data
  },
  (error) => {
    const userStore = useUserStore()
    
    if (error.response) {
      const { status, data } = error.response
      
      switch (status) {
        case 401:
          ElMessage.error('登录已过期，请重新登录')
          userStore.logout()
          router.push('/login')
          break
        case 400:
          if (data && data.message) {
            ElMessage.error(data.message)
          } else if (typeof data === 'object') {
            const errors = Object.values(data)
            if (errors.length > 0) {
              ElMessage.error(errors[0])
            }
          }
          break
        case 500:
          ElMessage.error('服务器内部错误')
          break
        default:
          if (data && data.message) {
            ElMessage.error(data.message)
          }
      }
    } else {
      ElMessage.error('网络错误，请检查网络连接')
    }
    
    return Promise.reject(error)
  }
)

export default request
