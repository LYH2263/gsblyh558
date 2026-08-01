package com.exam.system.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "exam_records")
public class ExamRecord {
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
    @JoinColumn(name = "reservation_id", unique = true)
    @JsonIgnore
    private SessionReservation reservation;

    private LocalDateTime startTime;

    private LocalDateTime submitTime;

    private Integer score;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private Integer tabSwitchCount = 0;

    @OneToMany(mappedBy = "examRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExamAnswerDetail> details;
}
