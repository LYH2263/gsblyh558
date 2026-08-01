package com.exam.system.repository;

import com.exam.system.entity.ExamRecord;
import com.exam.system.entity.ExamSession;
import com.exam.system.entity.SessionReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExamRecordRepository extends JpaRepository<ExamRecord, Long> {
    boolean existsByReservation(SessionReservation reservation);

    Optional<ExamRecord> findByReservation(SessionReservation reservation);

    Optional<ExamRecord> findByReservationId(Long reservationId);

    long countBySession(ExamSession session);

    List<ExamRecord> findBySession(ExamSession session);
}
