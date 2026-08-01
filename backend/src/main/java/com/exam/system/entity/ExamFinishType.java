package com.exam.system.entity;

/**
 * 交卷类型：正常交卷 / 到点（超时）交卷 / 因切屏被强制交卷。
 * TIMEOUT 与 FORCED 均由服务端独立判定，即使前端不提交或篡改也会被判定。
 */
public enum ExamFinishType {
    NORMAL,
    TIMEOUT,
    FORCED
}
