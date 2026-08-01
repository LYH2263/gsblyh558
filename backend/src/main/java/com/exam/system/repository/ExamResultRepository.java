package com.exam.system.repository;

import com.exam.system.entity.ExamResult;
import com.exam.system.entity.FinishType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExamResultRepository extends JpaRepository<ExamResult, Long> {
    List<ExamResult> findByUserId(Long userId);
    List<ExamResult> findByExamId(Long examId);

    boolean existsByUser_IdAndSession_Id(Long userId, Long sessionId);

    long countBySession_Id(Long sessionId);

    long countBySession_IdAndFinishType(Long sessionId, FinishType finishType);

    @Query("SELECT COALESCE(AVG(r.usedMinutes), 0) FROM ExamResult r WHERE r.session.id = :sessionId")
    Double avgUsedMinutesBySessionId(@Param("sessionId") Long sessionId);
}
