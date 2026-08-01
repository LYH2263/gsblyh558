import request from '../utils/request'

export const listAdminSessions = (params) =>
  request.get('/api/admin/sessions', { params: params || {} }).then(res => res.data)

export const createAdminSession = (payload) =>
  request.post('/api/admin/sessions', payload).then(res => res.data)

export const updateAdminSession = (sessionId, payload) =>
  request.put(`/api/admin/sessions/${sessionId}`, payload).then(res => res.data)

export const offlineAdminSession = (sessionId) =>
  request.post(`/api/admin/sessions/${sessionId}/offline`).then(res => res.data)

export const listSessionReservations = (sessionId) =>
  request.get(`/api/admin/sessions/${sessionId}/reservations`).then(res => res.data)

export const getSessionDashboard = (status) =>
  request.get('/api/admin/sessions/dashboard', {
    params: status ? { status } : {}
  }).then(res => res.data)
