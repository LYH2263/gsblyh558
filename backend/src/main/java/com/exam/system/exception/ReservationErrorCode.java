package com.exam.system.exception;

/**
 * 场次预约业务错误码。遵循附录 A5：统一使用 RSV_ 前缀 + 大写下划线语义名。
 * 错误码字符串会放入响应体，前端据此展示中文提示，不依赖后端中文消息字符串匹配。
 */
public enum ReservationErrorCode {

    /** 场次名额已满 */
    RSV_SESSION_FULL("场次名额已满，无法预约"),

    /** 重复预约同一场次 */
    RSV_ALREADY_BOOKED("您已预约该场次，请勿重复预约"),

    /** 低于开考前 30 分钟的时间门槛 */
    RSV_BELOW_TIME_THRESHOLD("距离开考不足 30 分钟，无法预约"),

    /** 场次未开放预约（非 OPEN 状态） */
    RSV_SESSION_NOT_OPEN("该场次当前未开放预约"),

    /** 预约记录不存在或不属于当前用户 */
    RSV_RESERVATION_NOT_FOUND("未找到对应的预约记录"),

    /** 场次不存在 */
    RSV_SESSION_NOT_FOUND("未找到对应的考试场次"),

    /** 开考准入：用户未预约该场次 */
    RSV_ACCESS_NOT_BOOKED("您尚未预约该场次，无法进入考试"),

    /** 开考准入：场次尚未开始 */
    RSV_ACCESS_NOT_STARTED("考试尚未开始，请在开考后进入"),

    /** 开考准入：场次已结束（超出可作答/可进入窗口） */
    RSV_ACCESS_ENDED("本场次考试已结束，无法进入"),

    /** 开考准入：预约已被取消 */
    RSV_ACCESS_CANCELLED("您对该场次的预约已取消，无法进入"),

    /** 提交时服务端判定已超出作答窗口 */
    RSV_SUBMIT_TIMEOUT("已超出本场次作答时间，本次作答按超时处理"),

    /** 累计切屏达到上限，被强制交卷 */
    RSV_FORCED_SUBMIT_SWITCH("切屏次数已达上限，本次作答被强制交卷");

    private final String defaultMessage;

    ReservationErrorCode(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
