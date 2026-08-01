import request from '../utils/request'

export const listOpenSessions = () =>
  request.get('/api/sessions').then(res => res.data)

export const bookSession = (sessionId) =>
  request.post(`/api/sessions/${sessionId}/book`).then(res => res.data)

export const enterSession = (sessionId) =>
  request.post(`/api/sessions/${sessionId}/enter`).then(res => res.data)

export const submitSessionExam = (sessionId, payload) =>
  request.post(`/api/sessions/${sessionId}/submit`, payload).then(res => res.data)

export const reportScreenSwitch = (sessionId, payload) =>
  request.post(`/api/sessions/${sessionId}/screen-switch`, payload).then(res => res.data)
