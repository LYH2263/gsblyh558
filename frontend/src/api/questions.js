import request from '../utils/request'

export const listQuestions = (params) =>
  request.get('/api/questions', { params }).then(res => res.data)

export const createQuestion = (payload) =>
  request.post('/api/questions', payload).then(res => res.data)

export const updateQuestion = (id, payload) =>
  request.put(`/api/questions/${id}`, payload).then(res => res.data)

export const deleteQuestion = (id) =>
  request.delete(`/api/questions/${id}`).then(res => res.data)
