package com.exam.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Data
@Entity
@Table(name = "exam_questions")
public class ExamQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "exam_id")
    @JsonIgnore
    private Exam exam;

    @ManyToOne
    @JoinColumn(name = "question_id")
    private Question question;

    private Integer score;

    private Integer orderNum;
}
