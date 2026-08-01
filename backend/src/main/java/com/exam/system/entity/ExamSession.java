package com.exam.system.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 考试场次。一场已有考试可以派生出多个可预约场次。
 * 遵循工程公约第 3 条：一切时间量以「分钟」为单位的整数表达，字段名以 Minutes 结尾。
 */
@Data
@Entity
@Table(name = "exam_sessions")
public class ExamSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 场次所属的考试。
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    /**
     * 场次开始时间。
     */
    @NotNull(message = "必须设定场次开始时间")
    private LocalDateTime startTime;

    /**
     * 考试时长（分钟）。
     */
    @NotNull(message = "必须设定考试时长")
    @Min(value = 1, message = "考试时长必须大于 0 分钟")
    private Integer durationMinutes;

    /**
     * 迟到仍可进入的宽限量（分钟）。作答窗口 = [startTime, startTime + lateEntryMinutes] 可进入，
     * 作答截止时间 = startTime + durationMinutes。遵循第 3 条：分钟整数，字段名以 Minutes 结尾。
     */
    @NotNull(message = "必须设定迟到宽限量")
    @Min(value = 0, message = "迟到宽限量不能为负")
    private Integer lateEntryMinutes = 10;

    /**
     * 最大预约人数。
     */
    @NotNull(message = "必须设定最大预约人数")
    @Min(value = 1, message = "最大预约人数必须大于 0")
    private Integer capacity;

    @NotNull(message = "必须设定场次状态")
    @Enumerated(EnumType.STRING)
    private SessionStatus status;

    @CreationTimestamp
    private LocalDateTime createTime;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<SessionReservation> reservations;
}
