package com.exam.system.repository;

import com.exam.system.entity.QuestionCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionCategoryRepository extends JpaRepository<QuestionCategory, Long> {
}
