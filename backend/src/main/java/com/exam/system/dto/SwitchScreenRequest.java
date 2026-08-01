package com.exam.system.dto;

import lombok.Data;

import java.util.Map;

/**
 * 切屏次数上报。携带当前已作答内容，供达到阈值被强制交卷时按已答内容计分。
 */
@Data
public class SwitchScreenRequest {

    /** 当前已作答内容（questionId -> answer），可为空。 */
    private Map<Long, String> answers;
}
