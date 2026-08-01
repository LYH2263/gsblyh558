package com.exam.system.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReservationRequest {
    @NotNull(message = "必须指定预约的场次")
    private Long sessionId;
}
