import request from '../utils/request'

export const listMyReservations = () => request.get('/api/reservations/me').then(res => res.data)

export const getReservationAccess = (reservationId) => request.get(`/api/reservations/${reservationId}/access`).then(res => res.data)

export const reportTabSwitch = (reservationId) => request.post(`/api/reservations/${reservationId}/tab-switch`).then(res => res.data)

export const createReservation = (sessionId) => request.post(`/api/reservations/${sessionId}`).then(res => res.data)

export const cancelReservation = (id) => request.delete(`/api/reservations/${id}`).then(res => res.data)
