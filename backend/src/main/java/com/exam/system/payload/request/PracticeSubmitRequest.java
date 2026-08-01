package com.exam.system.payload.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PracticeSubmitRequest {
    @NotNull(message = "Question ID is required")
    private Long questionId;

    @NotNull(message = "User answer is required")
    private String userAnswer;
}
