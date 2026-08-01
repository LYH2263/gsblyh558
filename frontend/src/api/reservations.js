import request from '../utils/request'

export const listMyReservations = () =>
  request.get('/api/reservations/my').then(res => res.data)

export const cancelReservation = (reservationId) =>
  request.post(`/api/reservations/${reservationId}/cancel`).then(res => res.data)
