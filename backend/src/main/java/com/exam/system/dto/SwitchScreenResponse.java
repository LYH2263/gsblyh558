package com.exam.system.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 切屏上报响应：返回累计次数、上限以及是否应立即强制交卷。
 */
@Data
@Builder
public class SwitchScreenResponse {
    private Integer switchCount;
    private Integer maxSwitchCount;
    /** 累计切屏次数是否已达上限（达到即强制交卷） */
    private boolean forceSubmit;
}
