package com.exam.system.repository;

import com.exam.system.entity.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    Page<Question> findByCategoryId(Long categoryId, Pageable pageable);

    @Query(value = "SELECT * FROM questions WHERE category_id = :categoryId ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    List<Question> findRandomByCategoryId(@Param("categoryId") Long categoryId, @Param("limit") int limit);
}
