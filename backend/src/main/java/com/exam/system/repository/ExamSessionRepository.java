package com.exam.system.repository;

import com.exam.system.entity.ExamSession;
import com.exam.system.entity.SessionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ExamSessionRepository extends JpaRepository<ExamSession, Long> {

    List<ExamSession> findByStatus(SessionStatus status);

    List<ExamSession> findByExamId(Long examId);

    /**
     * 悲观写锁读取场次，用于并发预约时防止超卖。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ExamSession s WHERE s.id = :id")
    Optional<ExamSession> findByIdForUpdate(@Param("id") Long id);
}
