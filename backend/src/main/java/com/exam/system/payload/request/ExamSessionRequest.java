package com.exam.system.payload.request;

import com.exam.system.entity.SessionStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端创建 / 修改场次的入参。
 * 遵循第 3 条：时间量以分钟为单位整数表达，字段名以 Minutes 结尾。
 */
@Data
public class ExamSessionRequest {

    @NotNull(message = "必须指定所属考试")
    private Long examId;

    @NotNull(message = "必须设定场次开始时间")
    private LocalDateTime startTime;

    @NotNull(message = "必须设定考试时长")
    @Min(value = 1, message = "考试时长必须大于 0 分钟")
    private Integer durationMinutes;

    /**
     * 迟到仍可进入的宽限量（分钟），默认 10。遵循第 3 条：分钟整数，字段名以 Minutes 结尾。
     */
    @NotNull(message = "必须设定迟到宽限量")
    @Min(value = 0, message = "迟到宽限量不能为负")
    private Integer lateEntryMinutes = 10;

    @NotNull(message = "必须设定最大预约人数")
    @Min(value = 1, message = "最大预约人数必须大于 0")
    private Integer capacity;

    @NotNull(message = "必须设定场次状态")
    private SessionStatus status;
}
