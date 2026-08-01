package com.exam.system.repository;

import com.exam.system.entity.ExamResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamResultRepository extends JpaRepository<ExamResult, Long> {
    List<ExamResult> findByUserId(Long userId);
    List<ExamResult> findByExamId(Long examId);

    @Query("select r from ExamResult r where r.session.id = :sessionId")
    List<ExamResult> findBySessionId(@Param("sessionId") Long sessionId);

    long countBySessionIdAndTimeoutTrue(Long sessionId);

    long countBySessionIdAndScreenSwitchForcedTrue(Long sessionId);
}
