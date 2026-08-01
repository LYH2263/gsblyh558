package com.exam.system.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.Map;

@Data
public class ExamSubmission {
    private Long examId;
    
    @NotNull(message = "Answers cannot be null")
    private Map<Long, String> answers; // questionId -> answer
}
