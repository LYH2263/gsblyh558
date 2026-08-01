package com.exam.system.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 监考相关的可配置项，避免阈值散落成魔法数字。
 * 与第 2 轮的宽限量做法一致（后端可配置），通过 application.yml 的 exam.proctor.* 覆盖默认值。
 */
@Data
@Component
@ConfigurationProperties(prefix = "exam.proctor")
public class ExamProctorProperties {

    /**
     * 强制交卷的累计切屏阈值（次）。达到该次数后端拒绝继续作答并强制交卷。默认 3。
     */
    private int forceSubmitSwitchCount = 3;

    /**
     * 新建场次时默认的迟到宽限量（分钟），沿用第 2 轮口径。默认 10。
     */
    private int defaultLateEntryMinutes = 10;
}
