package com.exam.system.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "exam_sessions")
public class ExamSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "必须关联一场考试")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    @JsonIgnore
    private Exam exam;

    @NotNull(message = "必须设定开始时间")
    @Column(nullable = false)
    private LocalDateTime startTime;

    @NotNull(message = "必须设定考试时长")
    @Min(value = 1, message = "考试时长必须大于0")
    @Column(nullable = false)
    private Integer durationMinutes;

    @NotNull(message = "必须设定最大预约人数")
    @Min(value = 1, message = "最大预约人数必须大于0")
    @Column(nullable = false)
    private Integer capacity;

    /** 迟到宽限量（分钟）：场次开始后该分钟内仍可进入作答，默认 10 分钟 */
    @NotNull(message = "必须设定迟到宽限量")
    @Min(value = 0, message = "迟到宽限量不能为负数")
    @Column(nullable = false)
    private Integer lateEntryMinutes = 10;

    /** 切屏次数上限：达到该次数即强制交卷，默认 3 次 */
    @NotNull(message = "必须设定切屏次数上限")
    @Min(value = 1, message = "切屏次数上限必须大于0")
    @Column(nullable = false)
    private Integer maxSwitchCount = 3;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status = SessionStatus.DRAFT;
}
