import axios from 'axios'
import { useAuthStore } from '../stores/auth'
import router from '../router'

const service = axios.create({
  timeout: 10000
})

service.interceptors.request.use(
  config => {
    const authStore = useAuthStore()
    if (authStore.user && authStore.user.token) {
      config.headers['Authorization'] = 'Bearer ' + authStore.user.token
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

service.interceptors.response.use(
  response => {
    return response
  },
  error => {
    const authStore = useAuthStore()
    
    if (error.response) {
      switch (error.response.status) {
        case 401:
          // Unauthorized: clear token and redirect to login
          authStore.logout()
          router.push('/login')
          break
        case 403:
          // Forbidden: redirect to 403 page
          router.push('/403')
          break
        case 404:
          // Not Found: optional, maybe show toast or redirect
          console.error('Resource not found', error.response.config.url)
          break
        case 500:
          console.error('Server Error', error.response.data)
          break
        default:
          console.error('Other Error', error.response.status)
      }
    }
    return Promise.reject(error)
  }
)

export default service
