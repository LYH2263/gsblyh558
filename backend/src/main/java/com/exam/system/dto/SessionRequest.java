package com.exam.system.dto;

import com.exam.system.entity.SessionStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record SessionRequest(
        @NotNull(message = "考试ID不能为空")
        Long examId,

        @NotNull(message = "场次开始时间不能为空")
        LocalDateTime startTime,

        @NotNull(message = "考试时长不能为空")
        @Min(value = 1, message = "考试时长必须大于0")
        Integer durationMinutes,

        @NotNull(message = "最大预约人数不能为空")
        @Min(value = 1, message = "最大预约人数必须大于0")
        Integer capacity,

        @Min(value = 0, message = "迟到宽限分钟数不能为负数")
        Integer lateGraceMinutes,

        @Min(value = 1, message = "切屏强制交卷阈值必须大于0")
        Integer tabSwitchLimit,

        @NotNull(message = "场次状态不能为空")
        SessionStatus status
) {
}
