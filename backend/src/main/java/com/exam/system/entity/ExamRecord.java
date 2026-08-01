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

    private LocalDateTime startTime;

    private LocalDateTime submitTime;

    private Integer score;

    @OneToMany(mappedBy = "examRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExamAnswerDetail> details;
}
