package com.exam.system.repository;

import com.exam.system.entity.ExamSession;
import com.exam.system.entity.SessionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ExamSessionRepository extends JpaRepository<ExamSession, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ExamSession s where s.id = :id")
    Optional<ExamSession> findByIdForUpdate(@Param("id") Long id);

    List<ExamSession> findByStatusAndStartTimeAfterOrderByStartTimeAsc(SessionStatus status, LocalDateTime now);

    List<ExamSession> findAllByOrderByStartTimeDesc();

    List<ExamSession> findByExamIdOrderByStartTimeDesc(Long examId);

    List<ExamSession> findByStatusOrderByStartTimeDesc(SessionStatus status);

    List<ExamSession> findByExamIdAndStatusOrderByStartTimeDesc(Long examId, SessionStatus status);
}
