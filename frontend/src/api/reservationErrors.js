/**
 * 场次预约业务错误码 → 中文提示映射（附录 A5）。
 * 前端据后端返回的错误码字符串展示中文，不依赖后端中文消息字符串匹配。
 */
export const RESERVATION_ERROR_MESSAGES = {
  RSV_SESSION_FULL: '该场次名额已满，无法预约',
  RSV_ALREADY_BOOKED: '您已预约该场次，请勿重复预约',
  RSV_BELOW_TIME_THRESHOLD: '距离开考不足 30 分钟，无法预约',
  RSV_SESSION_NOT_OPEN: '该场次当前未开放预约',
  RSV_RESERVATION_NOT_FOUND: '未找到对应的预约记录',
  RSV_SESSION_NOT_FOUND: '未找到对应的考试场次',
  RSV_ACCESS_NOT_BOOKED: '您尚未预约该场次，无法进入考试',
  RSV_ACCESS_NOT_STARTED: '考试尚未开始，请在开考后再进入',
  RSV_ACCESS_ENDED: '本场次考试已结束，无法进入',
  RSV_ACCESS_CANCELLED: '您对该场次的预约已取消，无法进入',
  RSV_SUBMIT_TIMEOUT: '已超出本场次作答时间，本次作答按超时处理',
  RSV_FORCED_SUBMIT_SWITCH: '切屏次数已达上限，本次作答被强制交卷'
}

/**
 * 从 Axios 错误中提取后端错误码字符串（约定放在响应体 data 字段）。
 */
export function extractReservationErrorCode(error) {
  return error?.response?.data?.data || null
}

/**
 * 从 Axios 错误中解析后端错误码并返回中文提示。
 * 错误码约定放在响应体 data 字段。
 */
export function resolveReservationError(error, fallback = '操作失败，请稍后重试') {
  const code = error?.response?.data?.data
  if (code && RESERVATION_ERROR_MESSAGES[code]) {
    return RESERVATION_ERROR_MESSAGES[code]
  }
  return error?.response?.data?.message || fallback
}
