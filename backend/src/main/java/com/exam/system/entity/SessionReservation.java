package com.exam.system.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "session_reservations")
public class SessionReservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ExamSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime reservedAt;

    /** 首次进入答题页的时间（开考时间），未开考为空 */
    private LocalDateTime startedAt;

    /** 答题过程中累计切屏次数（达到场次上限将被强制交卷） */
    @Column(nullable = false)
    private Integer switchCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status = ReservationStatus.BOOKED;
}
