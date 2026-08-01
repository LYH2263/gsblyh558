import request from '../utils/request'

/**
 * 场次预约 · 用户端 API。
 * 遵循工程公约第 2 条：所有后端请求收敛在 api 目录下的领域模块中。
 */

// 浏览可预约场次（含剩余名额、是否已预约）
export function fetchOpenSessions() {
  return request.get('/api/sessions')
}

// 预约某场次
export function bookSession(sessionId) {
  return request.post('/api/reservations', null, { params: { sessionId } })
}

// 取消预约
export function cancelReservation(reservationId) {
  return request.post(`/api/reservations/${reservationId}/cancel`)
}

// 我的预约列表
export function fetchMyReservations() {
  return request.get('/api/reservations/mine')
}

// 开考准入校验：通过返回场次/预约定位与剩余作答时间；失败以 RSV_ 错误码返回
export function checkSessionAccess(sessionId) {
  return request.get(`/api/sessions/${sessionId}/access`)
}

// 上报一次切屏。携带当前已作答内容，达阈值时后端强制交卷并以错误码返回
export function reportSwitchScreen(reservationId, answers) {
  return request.post(`/api/reservations/${reservationId}/switch-screen`, { answers })
}
