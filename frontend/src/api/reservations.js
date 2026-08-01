import request from '../utils/request'

// 场次预约接口
export const reservationApi = {
  book(sessionId) {
    return request.post('/api/reservations', { sessionId })
  },
  my() {
    return request.get('/api/reservations/my')
  },
  cancel(id) {
    return request.put(`/api/reservations/${id}/cancel`)
  },
  // 切屏上报：返回累计次数、上限与是否强制交卷
  reportSwitch(id) {
    return request.post(`/api/reservations/${id}/switch-screen`)
  }
}
