package com.exam.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
@Entity
@Table(name = "question_categories")
public class QuestionCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Category name cannot be empty")
    @Column(nullable = false)
    private String name;

    private String description;

    private Long parentId;
}
