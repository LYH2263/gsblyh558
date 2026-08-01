import request from '../utils/request'

export const listQuestions = (params) => request.get('/api/questions', { params }).then(res => res.data)

export const createQuestion = (data) => request.post('/api/questions', data).then(res => res.data)

export const updateQuestion = (id, data) => request.put(`/api/questions/${id}`, data).then(res => res.data)

export const deleteQuestion = (id) => request.delete(`/api/questions/${id}`).then(res => res.data)
