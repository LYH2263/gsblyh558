package com.exam.system.repository;

import com.exam.system.entity.ReservationStatus;
import com.exam.system.entity.SessionReservation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessionReservationRepository extends JpaRepository<SessionReservation, Long> {

    long countBySessionIdAndStatus(Long sessionId, ReservationStatus status);

    long countBySessionIdAndStatusAndStartedAtIsNotNull(Long sessionId, ReservationStatus status);

    long countBySessionIdAndStatusAndForceSubmittedTrue(Long sessionId, ReservationStatus status);

    Optional<SessionReservation> findBySessionIdAndUserIdAndStatus(Long sessionId, Long userId, ReservationStatus status);

    boolean existsBySessionIdAndUserIdAndStatus(Long sessionId, Long userId, ReservationStatus status);

    List<SessionReservation> findByUserIdAndStatusOrderByReservedAtDesc(Long userId, ReservationStatus status);

    List<SessionReservation> findBySessionIdOrderByReservedAtDesc(Long sessionId);

    List<SessionReservation> findBySessionIdAndStatus(Long sessionId, ReservationStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from SessionReservation r where r.id = :id")
    Optional<SessionReservation> findByIdForUpdate(@Param("id") Long id);
}
