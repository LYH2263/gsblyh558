package com.exam.system.dto;

public record TabSwitchResponse(
        Integer count,
        Integer limit,
        boolean thresholdExceeded
) {
}
