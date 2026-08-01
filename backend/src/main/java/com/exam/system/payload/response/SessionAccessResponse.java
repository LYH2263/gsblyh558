package com.exam.system.payload.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 开考准入结果。准入通过时携带答题页所需的场次/预约定位信息与剩余作答时间。
 * 准入失败时通过 ReservationException + RSV_ 错误码返回，前端据错误码分别展示文案。
 */
@Data
public class SessionAccessResponse {

    private Long sessionId;
    private Long reservationId;
    private Long examId;
    private String examTitle;
    private LocalDateTime startTime;
    private Integer durationMinutes;
    /** 服务端计算的剩余作答时间（分钟，向下取整仅供展示参考，前端用秒级由 deadline 推算） */
    private Integer remainingMinutes;
    /** 剩余作答秒数，前端倒计时以此为准，避免跨端时钟误差 */
    private long remainingSeconds;
}
