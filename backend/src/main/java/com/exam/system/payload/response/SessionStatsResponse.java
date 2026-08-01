package com.exam.system.payload.response;

import com.exam.system.entity.SessionStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 场次数据看板：单场次统计。
 * 平均用时单位为分钟（遵循第 3 条）。
 */
@Data
public class SessionStatsResponse {

    private Long sessionId;
    private String examTitle;
    private LocalDateTime startTime;
    private SessionStatus status;

    /** 预约人数（BOOKED） */
    private long bookedCount;
    /** 实际开考人数（已记录 startedAt） */
    private long startedCount;
    /** 按时正常完成人数 */
    private long normalCount;
    /** 超时自动交卷人数 */
    private long timeoutCount;
    /** 因切屏被强制交卷人数 */
    private long forcedCount;
    /** 平均用时（分钟），无数据为 0 */
    private int averageUsedMinutes;
}
