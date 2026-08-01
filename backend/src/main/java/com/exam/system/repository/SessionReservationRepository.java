package com.exam.system.repository;

import com.exam.system.entity.ReservationStatus;
import com.exam.system.entity.SessionReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessionReservationRepository extends JpaRepository<SessionReservation, Long> {
    long countBySession_IdAndStatus(Long sessionId, ReservationStatus status);

    long countBySession_IdAndStartedAtIsNotNull(Long sessionId);

    boolean existsBySession_IdAndUser_IdAndStatus(Long sessionId, Long userId, ReservationStatus status);

    Optional<SessionReservation> findBySession_IdAndUser_Id(Long sessionId, Long userId);

    Optional<SessionReservation> findByIdAndUser_Id(Long id, Long userId);

    List<SessionReservation> findByUser_IdOrderByReservedAtDesc(Long userId);
}
