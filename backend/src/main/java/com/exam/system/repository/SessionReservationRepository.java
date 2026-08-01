package com.exam.system.repository;

import com.exam.system.entity.ExamSession;
import com.exam.system.entity.ReservationStatus;
import com.exam.system.entity.SessionReservation;
import com.exam.system.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SessionReservationRepository extends JpaRepository<SessionReservation, Long> {
    long countBySessionAndStatus(ExamSession session, ReservationStatus status);

    boolean existsBySessionAndUserAndStatus(ExamSession session, User user, ReservationStatus status);

    Optional<SessionReservation> findByIdAndStatus(Long id, ReservationStatus status);

    List<SessionReservation> findByUserAndStatusOrderByReservedAtDesc(User user, ReservationStatus status);
}
