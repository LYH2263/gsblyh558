package com.exam.system.dto;

import com.exam.system.entity.ExamResult;
import lombok.Data;

@Data
public class ScreenSwitchReportResponse {

    private Integer screenSwitchCount;
    private Integer screenSwitchLimit;
    private boolean limitReached;
    private ExamResult result;

    public static ScreenSwitchReportResponse of(int count, int limit) {
        ScreenSwitchReportResponse response = new ScreenSwitchReportResponse();
        response.setScreenSwitchCount(count);
        response.setScreenSwitchLimit(limit);
        response.setLimitReached(false);
        return response;
    }

    public static ScreenSwitchReportResponse forced(int count, int limit, ExamResult result) {
        ScreenSwitchReportResponse response = new ScreenSwitchReportResponse();
        response.setScreenSwitchCount(count);
        response.setScreenSwitchLimit(limit);
        response.setLimitReached(true);
        response.setResult(result);
        return response;
    }
}
