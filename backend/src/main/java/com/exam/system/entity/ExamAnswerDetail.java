package com.exam.system.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "exam_answer_details")
public class ExamAnswerDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "exam_record_id")
    @JsonIgnore
    private ExamRecord examRecord;

    @ManyToOne
    @JoinColumn(name = "question_id")
    private Question question;

    @Column(columnDefinition = "TEXT")
    private String userAnswer;

    private Boolean isCorrect;

    private Integer score;
}
