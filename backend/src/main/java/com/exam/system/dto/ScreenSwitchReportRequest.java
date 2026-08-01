package com.exam.system.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class ScreenSwitchReportRequest {

    @NotNull(message = "reservationId 不能为空")
    private Long reservationId;

    @NotNull(message = "sessionId 不能为空")
    private Long sessionId;

    private Map<Long, String> answers;
}
