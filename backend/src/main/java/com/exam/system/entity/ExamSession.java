package com.exam.system.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "exam_sessions")
public class ExamSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @NotNull(message = "场次开始时间不能为空")
    @Column(nullable = false)
    private LocalDateTime startTime;

    @NotNull(message = "考试时长不能为空")
    @Min(value = 1, message = "考试时长必须大于0")
    @Column(nullable = false)
    private Integer durationMinutes;

    @NotNull(message = "最大预约人数不能为空")
    @Min(value = 1, message = "最大预约人数必须大于0")
    @Column(nullable = false)
    private Integer capacity;

    @NotNull(message = "迟到宽限分钟数不能为空")
    @Min(value = 0, message = "迟到宽限分钟数不能为负数")
    @Column(nullable = false, columnDefinition = "integer default 10")
    private Integer lateGraceMinutes = 10;

    @NotNull(message = "切屏强制交卷阈值不能为空")
    @Min(value = 1, message = "切屏强制交卷阈值必须大于0")
    @Column(nullable = false, columnDefinition = "integer default 3")
    private Integer tabSwitchLimit = 3;

    @NotNull(message = "场次状态不能为空")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<SessionReservation> reservations;
}
