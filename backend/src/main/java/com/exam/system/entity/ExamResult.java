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

    /** 本次作答所属场次（通过场次预约进入时记录） */
    @ManyToOne
    @JoinColumn(name = "session_id")
    @JsonIgnore
    private ExamSession session;

    /** 交卷方式：按时完成 / 超时自动交卷 / 切屏强制交卷 */
    @Enumerated(EnumType.STRING)
    private FinishType finishType = FinishType.NORMAL;

    /** 作答用时（分钟）：从开考时间到交卷时间 */
    private Integer usedMinutes;

    private Integer score;

    @CreationTimestamp
    private LocalDateTime submitTime;
}
