package com.exam.system.dto;

import com.exam.system.entity.SessionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 场次数据看板行：预约/开考/按时完成/超时交卷/切屏强制交卷人数与平均用时。
 */
@Data
@Builder
public class SessionStatsResponse {
    private Long sessionId;
    private Long examId;
    private String examTitle;
    private LocalDateTime startTime;
    private Integer durationMinutes;
    private SessionStatus status;
    /** 预约人数 */
    private long bookedCount;
    /** 实际开考人数 */
    private long startedCount;
    /** 按时完成人数 */
    private long finishedCount;
    /** 超时自动交卷人数 */
    private long timeoutCount;
    /** 因切屏被强制交卷人数 */
    private long forcedCount;
    /** 平均作答用时（分钟，整数） */
    private long avgUsedMinutes;
}
