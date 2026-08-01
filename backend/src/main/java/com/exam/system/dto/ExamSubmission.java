package com.exam.system.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.Map;

@Data
public class ExamSubmission {
    private Long examId;

    /** 本次作答所属场次 ID（通过场次预约进入时必传） */
    private Long sessionId;

    /** 本次作答对应的预约记录 ID */
    private Long reservationId;

    @NotNull(message = "Answers cannot be null")
    private Map<Long, String> answers; // questionId -> answer
}
