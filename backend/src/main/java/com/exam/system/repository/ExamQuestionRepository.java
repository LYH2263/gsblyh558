package com.exam.system.repository;

import com.exam.system.entity.ExamQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamQuestionRepository extends JpaRepository<ExamQuestion, Long> {
}
