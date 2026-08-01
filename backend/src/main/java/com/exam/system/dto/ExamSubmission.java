package com.exam.system.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.Map;

@Data
public class ExamSubmission {
    private Long examId;

    /**
     * 本次作答所属场次与预约（命名沿用附录 A1）。用于服务端超时判定与结果落库定位。
     */
    private Long sessionId;
    private Long reservationId;

    /**
     * 前端标记是否为倒计时归零触发的自动提交（仅作参考，超时以服务端独立判定为准）。
     */
    private boolean autoSubmitted;

    @NotNull(message = "Answers cannot be null")
    private Map<Long, String> answers; // questionId -> answer
}
