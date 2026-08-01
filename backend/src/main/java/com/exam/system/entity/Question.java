package com.exam.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.CreationTimestamp;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "questions")
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private QuestionCategory category;

    @NotNull(message = "Question type is required")
    @Enumerated(EnumType.STRING)
    private QuestionType type;

    @NotBlank(message = "Content cannot be empty")
    @Column(columnDefinition = "TEXT")
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> options;

    @NotBlank(message = "Answer cannot be empty")
    @Column(columnDefinition = "TEXT")
    private String answer;

    @Column(columnDefinition = "TEXT")
    private String analysis;

    private Integer difficulty; // 1-5

    @CreationTimestamp
    private LocalDateTime createTime;
}
