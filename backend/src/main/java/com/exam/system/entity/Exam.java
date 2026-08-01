package com.exam.system.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.util.List;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@Entity
@Table(name = "exams")
public class Exam {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "考试标题不能为空")
    private String title;

    private String description;

    @NotNull(message = "必须设定考试总分")
    @Min(value = 1, message = "考试总分必须大于0")
    private Integer totalScore;

    @NotNull(message = "必须设定考试时长")
    @Min(value = 1, message = "考试时长必须大于0")
    private Integer duration; // minutes

    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderNum ASC")
    private List<ExamQuestion> questions;

    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<ExamResult> results;

    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<ExamRecord> records;
}
