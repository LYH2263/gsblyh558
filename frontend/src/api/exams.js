import request from '../utils/request'

export const listExams = () => request.get('/api/exams').then(res => res.data)

export const getExam = (id) => request.get(`/api/exams/${id}`).then(res => res.data)

export const createExam = (data) => request.post('/api/exams', data).then(res => res.data)

export const updateExam = (id, data) => request.put(`/api/exams/${id}`, data).then(res => res.data)

export const deleteExam = (id) => request.delete(`/api/exams/${id}`).then(res => res.data)

export const submitExam = (id, data) => request.post(`/api/exams/${id}/submit`, data).then(res => res.data)
