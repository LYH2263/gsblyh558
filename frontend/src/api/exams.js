import request from '../utils/request'

export const listExams = () => request.get('/api/exams').then(res => res.data)
export const getExam = (id) => request.get(`/api/exams/${id}`).then(res => res.data)
export const createExam = (payload) => request.post('/api/exams', payload).then(res => res.data)
export const updateExam = (id, payload) => request.put(`/api/exams/${id}`, payload).then(res => res.data)
export const deleteExam = (id) => request.delete(`/api/exams/${id}`).then(res => res.data)
export const submitExam = (id, payload) =>
  request.post(`/api/exams/${id}/submit`, payload).then(res => res.data)
