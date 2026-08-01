package com.exam.system.repository;

import com.exam.system.entity.ReservationStatus;
import com.exam.system.entity.SessionReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SessionReservationRepository extends JpaRepository<SessionReservation, Long> {

    long countBySessionIdAndStatus(Long sessionId, ReservationStatus status);

    /**
     * 已开考人数：状态为 BOOKED 且已记录 startedAt 的预约数。
     */
    long countBySessionIdAndStatusAndStartedAtIsNotNull(Long sessionId, ReservationStatus status);

    Optional<SessionReservation> findBySessionIdAndUserIdAndStatus(Long sessionId, Long userId, ReservationStatus status);

    Optional<SessionReservation> findByIdAndUserId(Long id, Long userId);

    List<SessionReservation> findByUserIdAndStatusOrderByReservedAtDesc(Long userId, ReservationStatus status);

    List<SessionReservation> findByUserIdOrderByReservedAtDesc(Long userId);
}
