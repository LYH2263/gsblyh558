const CODE_MESSAGE_MAP = {
  RSV_CAPACITY_FULL: '该场次名额已满，无法预约',
  RSV_DUPLICATE_BOOKING: '您已经预约过该场次，请勿重复预约',
  RSV_BELOW_LEAD_TIME: '距离场次开始不足 30 分钟，已无法预约',
  RSV_SESSION_NOT_OPEN: '该场次当前未开放预约',
  RSV_SESSION_NOT_FOUND: '未找到该场次',
  RSV_EXAM_NOT_FOUND: '未找到关联的考试',
  RSV_NO_RESERVATION: '您没有预约该场次，无法进入考试',
  RSV_RESERVATION_CANCELLED: '您的预约已取消，无法进入考试',
  RSV_LATE_GRACE_EXCEEDED: '已超过迟到入场宽限时间，无法进入考试',
  RSV_SESSION_NOT_STARTED: '该场次尚未开始，请稍后再进入',
  RSV_SESSION_ENDED: '该场次已结束，无法再进入或提交',
  RSV_SESSION_MISMATCH: '预约与场次信息不匹配',
  RSV_EXAM_MISMATCH: '考试信息不匹配',
  RSV_SESSION_HAS_BOOKINGS: '该场次已有用户预约，不能执行此操作',
  RSV_INVALID_CAPACITY: '容量不能小于当前已预约人数',
  RSV_SUBMISSION_TIMEOUT: '已超过作答时间，本次作答按超时记录',
  RSV_SCREEN_SWITCH_FORCED: '因切屏次数已达上限，本场考试已被强制交卷'
}

export function getErrorMessage(error, fallback = '操作失败，请稍后重试') {
  const errorCode = error?.response?.data?.errorCode
  if (errorCode && CODE_MESSAGE_MAP[errorCode]) {
    return CODE_MESSAGE_MAP[errorCode]
  }
  return error?.response?.data?.message || fallback
}
