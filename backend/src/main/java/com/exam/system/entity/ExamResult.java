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

    @ManyToOne
    @JoinColumn(name = "session_id")
    @JsonIgnore
    private ExamSession session;

    @ManyToOne
    @JoinColumn(name = "reservation_id")
    @JsonIgnore
    private SessionReservation reservation;

    private Integer score;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean timedOut = false;

    @Enumerated(EnumType.STRING)
    private ForcedSubmitReason forcedSubmitReason;

    @CreationTimestamp
    private LocalDateTime submitTime;
}
