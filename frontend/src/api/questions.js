import request from '../utils/request'

// 题库接口
export const questionApi = {
  list(params) {
    return request.get('/api/questions', { params })
  },
  create(payload) {
    return request.post('/api/questions', payload)
  },
  update(id, payload) {
    return request.put(`/api/questions/${id}`, payload)
  },
  remove(id) {
    return request.delete(`/api/questions/${id}`)
  }
}
