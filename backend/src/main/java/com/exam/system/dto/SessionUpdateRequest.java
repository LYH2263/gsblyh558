package com.exam.system.dto;

import com.exam.system.entity.SessionStatus;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SessionUpdateRequest {

    private LocalDateTime startTime;

    @Min(value = 1, message = "考试时长必须大于 0 分钟")
    private Integer durationMinutes;

    @Min(value = 1, message = "最大预约人数必须大于 0")
    private Integer capacity;

    @Min(value = 0, message = "迟到宽限量不能为负数")
    private Integer lateGraceMinutes;

    @Min(value = 1, message = "切屏强制交卷阈值必须大于 0")
    private Integer screenSwitchLimit;

    private SessionStatus status;
}
