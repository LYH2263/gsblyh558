import request from '../utils/request'

export const listAvailableSessions = () => request.get('/api/sessions').then(res => res.data)

export const listAdminSessions = (params = {}) => request.get('/api/admin/sessions', {
  params
}).then(res => res.data)

export const getSessionAnalytics = (status) => request.get('/api/admin/sessions/analytics', {
  params: status ? { status } : {}
}).then(res => res.data)

export const createSession = (data) => request.post('/api/admin/sessions', data).then(res => res.data)

export const updateSession = (id, data) => request.put(`/api/admin/sessions/${id}`, data).then(res => res.data)

export const closeSession = (id) => request.post(`/api/admin/sessions/${id}/close`).then(res => res.data)
