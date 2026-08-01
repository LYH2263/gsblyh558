import request from '../utils/request'

// 练习接口
export const practiceApi = {
  submit(payload) {
    return request.post('/api/practice/submit', payload)
  }
}
