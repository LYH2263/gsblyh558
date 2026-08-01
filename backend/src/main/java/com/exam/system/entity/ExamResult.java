package com.exam.system.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "exam_results")
public class ExamResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "exam_id")
    @JsonIgnore
    private Exam exam;

    /**
     * 本次作答关联的场次（附录 A1）。为空表示非场次预约途径的作答（兼容旧流程）。
     */
    @ManyToOne
    @JoinColumn(name = "session_id")
    @JsonIgnore
    private ExamSession session;

    /**
     * 本次作答关联的预约记录（附录 A1）。
     */
    @ManyToOne
    @JoinColumn(name = "reservation_id")
    @JsonIgnore
    private SessionReservation reservation;

    private Integer score;

    /**
     * 交卷类型：正常 / 超时 / 因切屏强制。超时与强制均由服务端独立判定。
     */
    @Enumerated(EnumType.STRING)
    private ExamFinishType finishType = ExamFinishType.NORMAL;

    /**
     * 本次作答用时（分钟，遵循第 3 条）。从场次开始到交卷的时长；非场次途径为空。
     */
    private Integer usedMinutes;

    @CreationTimestamp
    private LocalDateTime submitTime;
}
