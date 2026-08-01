package com.exam.system.dto;

import com.exam.system.entity.ForcedSubmitReason;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.Map;

@Data
public class ExamSubmission {
    private Long examId;

    private Long sessionId;

    private Long reservationId;

    private ForcedSubmitReason forcedSubmitReason;

    @NotNull(message = "Answers cannot be null")
    private Map<Long, String> answers;
}
