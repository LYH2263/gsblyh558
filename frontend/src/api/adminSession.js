import request from '../utils/request'

/**
 * 场次管理 · 管理端 API。
 * 遵循工程公约第 2 条：所有后端请求收敛在 api 目录下的领域模块中。
 */

// 管理端场次列表（含已预约人数、剩余名额、已开考人数）。可按 SessionStatus 过滤
export function fetchAdminSessions(status) {
  const params = status ? { status } : {}
  return request.get('/api/admin/sessions', { params })
}

// 场次数据看板统计。可按 SessionStatus 过滤
export function fetchSessionStats(status) {
  const params = status ? { status } : {}
  return request.get('/api/admin/sessions/stats', { params })
}

// 创建场次
export function createSession(payload) {
  return request.post('/api/admin/sessions', payload)
}

// 修改场次
export function updateSession(id, payload) {
  return request.put(`/api/admin/sessions/${id}`, payload)
}

// 下线（关闭）场次。confirmed=false 时若已有人预约，后端返回体携带 bookedCount 供人数提示
export function closeSession(id, confirmed = false) {
  return request.post(`/api/admin/sessions/${id}/close`, null, { params: { confirmed } })
}
