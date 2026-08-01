package com.exam.system.payload.response;

import lombok.Data;

/**
 * 切屏上报结果。返回当前累计切屏次数、阈值与是否已被强制交卷。
 */
@Data
public class SwitchScreenResponse {

    /** 当前累计切屏次数 */
    private int switchScreenCount;

    /** 强制交卷阈值（可配置） */
    private int threshold;

    /** 是否已达到阈值并被强制交卷 */
    private boolean forcedSubmitted;
}
