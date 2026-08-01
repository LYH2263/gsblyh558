package com.exam.system.repository;

import com.exam.system.entity.ExamSession;
import com.exam.system.entity.SessionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamSessionRepository extends JpaRepository<ExamSession, Long> {

    List<ExamSession> findByExamIdOrderByStartTimeAsc(Long examId);

    List<ExamSession> findByExamIdAndStatusOrderByStartTimeAsc(Long examId, SessionStatus status);

    List<ExamSession> findByStatusOrderByStartTimeAsc(SessionStatus status);

    List<ExamSession> findAllByOrderByStartTimeAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ExamSession s where s.id = :id")
    Optional<ExamSession> findByIdForUpdate(@Param("id") Long id);
}
