import request from '../utils/request'

// 考试接口
export const examApi = {
  list() {
    return request.get('/api/exams')
  },
  get(id) {
    return request.get(`/api/exams/${id}`)
  },
  create(payload) {
    return request.post('/api/exams', payload)
  },
  update(id, payload) {
    return request.put(`/api/exams/${id}`, payload)
  },
  remove(id) {
    return request.delete(`/api/exams/${id}`)
  },
  submit(id, payload) {
    return request.post(`/api/exams/${id}/submit`, payload)
  }
}
