import request from '../utils/request'

// 认证相关接口
export const authApi = {
  signin(username, password) {
    return request.post('/api/auth/signin', { username, password })
  },
  signup(user) {
    return request.post('/api/auth/signup', user)
  }
}
