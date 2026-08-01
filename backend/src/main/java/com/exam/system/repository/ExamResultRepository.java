package com.exam.system.repository;

import com.exam.system.entity.ExamResult;
import com.exam.system.entity.ExamSession;
import com.exam.system.entity.ForcedSubmitReason;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExamResultRepository extends JpaRepository<ExamResult, Long> {
    List<ExamResult> findByUserId(Long userId);
    List<ExamResult> findByExamId(Long examId);
    List<ExamResult> findBySession(ExamSession session);
    long countBySessionAndTimedOut(ExamSession session, Boolean timedOut);
    long countBySessionAndForcedSubmitReason(ExamSession session, ForcedSubmitReason reason);
}
