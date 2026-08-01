import request from '../utils/request'

// 题目分类接口
export const categoryApi = {
  list() {
    return request.get('/api/categories')
  },
  create(payload) {
    return request.post('/api/categories', payload)
  }
}
