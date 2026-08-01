package com.exam.system.dto;

import com.exam.system.entity.SessionStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SessionCreateRequest {

    @NotNull(message = "必须关联一场考试")
    private Long examId;

    @NotNull(message = "场次开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "考试时长不能为空")
    @Min(value = 1, message = "考试时长必须大于 0 分钟")
    private Integer durationMinutes;

    @NotNull(message = "最大预约人数不能为空")
    @Min(value = 1, message = "最大预约人数必须大于 0")
    private Integer capacity;

    @Min(value = 0, message = "迟到宽限量不能为负数")
    private Integer lateGraceMinutes;

    @Min(value = 1, message = "切屏强制交卷阈值必须大于 0")
    private Integer screenSwitchLimit;

    @NotNull(message = "场次状态不能为空")
    private SessionStatus status;
}
