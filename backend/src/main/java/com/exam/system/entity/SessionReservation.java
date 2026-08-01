package com.exam.system.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 场次预约记录。同一用户对同一场次的有效预约（BOOKED）唯一。
 */
@Data
@Entity
@Table(name = "session_reservations")
public class SessionReservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "session_id", nullable = false)
    @JsonIgnore
    private ExamSession session;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @CreationTimestamp
    private LocalDateTime reservedAt;

    /**
     * 取消时间，取消时写入。
     */
    private LocalDateTime cancelledAt;

    /**
     * 首次进入答题页（开考）的时间，用于统计「已开考人数」。
     */
    private LocalDateTime startedAt;

    /**
     * 累计切屏次数。前端每次切走上报一次，与本预约记录关联落库，不只存在前端内存。
     */
    @Column(nullable = false)
    private Integer switchScreenCount = 0;

    @Enumerated(EnumType.STRING)
    private ReservationStatus status;
}
