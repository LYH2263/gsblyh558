package com.exam.system.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExamSessionRequest {
    @NotNull(message = "必须指定所属考试")
    private Long examId;

    @NotNull(message = "必须设定开始时间")
    private LocalDateTime startTime;

    @NotNull(message = "必须设定考试时长")
    @Min(value = 1, message = "考试时长必须大于0")
    private Integer durationMinutes;

    @NotNull(message = "必须设定最大预约人数")
    @Min(value = 1, message = "最大预约人数必须大于0")
    private Integer capacity;

    /** 迟到宽限量（分钟），不传时默认为 10 */
    @Min(value = 0, message = "迟到宽限量不能为负数")
    private Integer lateEntryMinutes;

    /** 切屏次数上限，达到即强制交卷，不传时默认为 3 */
    @Min(value = 1, message = "切屏次数上限必须大于0")
    private Integer maxSwitchCount;
}
