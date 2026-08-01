package com.exam.system.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "exam_sessions")
public class ExamSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "必须关联一场考试")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @NotNull(message = "场次开始时间不能为空")
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @NotNull(message = "考试时长不能为空")
    @Min(value = 1, message = "考试时长必须大于 0 分钟")
    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @NotNull(message = "最大预约人数不能为空")
    @Min(value = 1, message = "最大预约人数必须大于 0")
    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @NotNull(message = "迟到宽限量不能为空")
    @Min(value = 0, message = "迟到宽限量不能为负数")
    @Column(name = "late_grace_minutes", nullable = false)
    private Integer lateGraceMinutes;

    @NotNull(message = "切屏强制交卷阈值不能为空")
    @Min(value = 1, message = "切屏强制交卷阈值必须大于 0")
    @Column(name = "screen_switch_limit", nullable = false)
    private Integer screenSwitchLimit;

    @NotNull(message = "场次状态不能为空")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SessionStatus status;

    @Version
    @Column(name = "version")
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = false)
    @JsonIgnore
    private List<SessionReservation> reservations;
}
