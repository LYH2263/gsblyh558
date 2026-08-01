package com.exam.system.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "session_reservations",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_session_user_active",
                        columnNames = {"session_id", "user_id", "status"})
        })
public class SessionReservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "必须关联场次")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private ExamSession session;

    @NotNull(message = "必须关联用户")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull(message = "预约时间不能为空")
    @Column(name = "reserved_at", nullable = false)
    private LocalDateTime reservedAt;

    @NotNull(message = "预约状态不能为空")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReservationStatus status;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "screen_switch_count", nullable = false)
    private Integer screenSwitchCount = 0;

    @Column(name = "force_submitted", nullable = false)
    private Boolean forceSubmitted = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
