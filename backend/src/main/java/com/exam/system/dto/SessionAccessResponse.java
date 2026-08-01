package com.exam.system.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 开考准入校验通过的响应：包含作答窗口信息，供前端渲染倒计时与提交定位。
 */
@Data
@Builder
public class SessionAccessResponse {
    private Long sessionId;
    private Long reservationId;
    private Long examId;
    private String examTitle;
    private LocalDateTime startTime;
    private Integer durationMinutes;
    private Integer lateEntryMinutes;
    /** 切屏次数上限（达到即强制交卷） */
    private Integer maxSwitchCount;
    /** 作答窗口截止时间（场次开始时间 + 考试时长） */
    private LocalDateTime endTime;
    /** 距作答窗口截止的剩余分钟数 */
    private long remainingMinutes;
}
