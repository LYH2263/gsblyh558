package com.exam.system.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "wrong_question_records")
public class WrongQuestionRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "question_id")
    private Question question;

    private Boolean isRemoved = false;

    private LocalDateTime lastWrongTime;

    private LocalDateTime removeTime;
}
