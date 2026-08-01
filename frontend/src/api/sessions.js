import request from '../utils/request'

// 考试场次接口
export const sessionApi = {
  // 用户端：可预约场次列表（含剩余名额）
  listBookable(examId) {
    return request.get('/api/sessions', { params: examId ? { examId } : {} })
  },
  // 用户端：开考准入校验（已预约 + 作答窗口内）
  checkAccess(sessionId) {
    return request.get(`/api/sessions/${sessionId}/access`)
  },
  // 管理端：全部场次列表（含已预约/已开考人数），支持按状态过滤
  adminList(examId, status) {
    const params = {}
    if (examId) params.examId = examId
    if (status) params.status = status
    return request.get('/api/admin/sessions', { params })
  },
  create(payload) {
    return request.post('/api/admin/sessions', payload)
  },
  update(id, payload) {
    return request.put(`/api/admin/sessions/${id}`, payload)
  },
  close(id) {
    return request.put(`/api/admin/sessions/${id}/close`)
  },
  // 管理端：场次数据看板统计
  adminStats(examId, status) {
    const params = {}
    if (examId) params.examId = examId
    if (status) params.status = status
    return request.get('/api/admin/sessions/stats', { params })
  }
}
