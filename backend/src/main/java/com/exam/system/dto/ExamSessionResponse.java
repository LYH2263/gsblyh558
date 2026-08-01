package com.exam.system.dto;

import com.exam.system.entity.SessionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ExamSessionResponse {
    private Long id;
    private Long examId;
    private String examTitle;
    private LocalDateTime startTime;
    private Integer durationMinutes;
    private Integer capacity;
    private Integer lateEntryMinutes;
    private Integer maxSwitchCount;
    private SessionStatus status;
    private long bookedCount;
    /** 已开考人数（已进入答题页的预约数） */
    private long startedCount;
    private long remainingQuota;
    /** 距场次开始的剩余分钟数（负数表示已开始） */
    private long minutesUntilStart;
    /** 当前是否可预约（开放中、有余量、且满足提前 30 分钟门槛） */
    private boolean bookable;
}
