package com.exam.system.service;

import com.exam.system.dto.OfflineSessionResponse;
import com.exam.system.dto.ReservationResponse;
import com.exam.system.dto.SessionCreateRequest;
import com.exam.system.dto.SessionResponse;
import com.exam.system.dto.SessionUpdateRequest;
import com.exam.system.entity.Exam;
import com.exam.system.entity.ExamSession;
import com.exam.system.entity.ReservationStatus;
import com.exam.system.entity.SessionReservation;
import com.exam.system.entity.SessionStatus;
import com.exam.system.entity.User;
import com.exam.system.exception.BusinessException;
import com.exam.system.exception.ReservationErrorCode;
import com.exam.system.exception.ResourceNotFoundException;
import com.exam.system.repository.ExamRepository;
import com.exam.system.repository.ExamSessionRepository;
import com.exam.system.repository.SessionReservationRepository;
import com.exam.system.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ExamSessionService {

    private static final int MIN_LEAD_MINUTES = 30;
    private static final int DEFAULT_LATE_GRACE_MINUTES = 10;
    private static final int DEFAULT_SCREEN_SWITCH_LIMIT = 3;

    @Autowired
    private ExamSessionRepository sessionRepository;

    @Autowired
    private SessionReservationRepository reservationRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public SessionResponse createSession(SessionCreateRequest request) {
        Exam exam = examRepository.findById(request.getExamId())
                .orElseThrow(() -> new BusinessException(
                        ReservationErrorCode.RSV_EXAM_NOT_FOUND,
                        "未找到关联的考试",
                        404));

        if (request.getStartTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(
                    ReservationErrorCode.RSV_BELOW_LEAD_TIME,
                    "场次开始时间不能早于当前时间");
        }

        ExamSession session = new ExamSession();
        session.setExam(exam);
        session.setStartTime(request.getStartTime());
        session.setDurationMinutes(request.getDurationMinutes());
        session.setCapacity(request.getCapacity());
        session.setLateGraceMinutes(request.getLateGraceMinutes() != null
                ? request.getLateGraceMinutes() : DEFAULT_LATE_GRACE_MINUTES);
        session.setScreenSwitchLimit(request.getScreenSwitchLimit() != null
                ? request.getScreenSwitchLimit() : DEFAULT_SCREEN_SWITCH_LIMIT);
        session.setStatus(request.getStatus() != null ? request.getStatus() : SessionStatus.DRAFT);

        ExamSession saved = sessionRepository.save(session);
        log.info("Admin created exam session [ID: {}] for exam [ID: {}]", saved.getId(), exam.getId());
        return SessionResponse.from(saved, 0, 0);
    }

    @Transactional
    public SessionResponse updateSession(Long sessionId, SessionUpdateRequest request) {
        ExamSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(
                        ReservationErrorCode.RSV_SESSION_NOT_FOUND,
                        "未找到该场次",
                        404));

        long bookedCount = reservationRepository.countBySessionIdAndStatus(sessionId, ReservationStatus.BOOKED);

        if (request.getCapacity() != null) {
            if (request.getCapacity() < bookedCount) {
                throw new BusinessException(
                        ReservationErrorCode.RSV_INVALID_CAPACITY,
                        "容量不能小于当前已预约人数 (" + bookedCount + ")");
            }
            session.setCapacity(request.getCapacity());
        }
        if (request.getStartTime() != null) {
            session.setStartTime(request.getStartTime());
        }
        if (request.getDurationMinutes() != null) {
            session.setDurationMinutes(request.getDurationMinutes());
        }
        if (request.getLateGraceMinutes() != null) {
            session.setLateGraceMinutes(request.getLateGraceMinutes());
        }
        if (request.getScreenSwitchLimit() != null) {
            session.setScreenSwitchLimit(request.getScreenSwitchLimit());
        }
        if (request.getStatus() != null) {
            session.setStatus(request.getStatus());
        }

        ExamSession updated = sessionRepository.save(session);
        log.info("Admin updated exam session [ID: {}]", sessionId);
        return SessionResponse.from(updated, (int) bookedCount,
                (int) reservationRepository.countBySessionIdAndStatusAndStartedAtIsNotNull(
                        sessionId, ReservationStatus.BOOKED));
    }

    @Transactional
    public OfflineSessionResponse offlineSession(Long sessionId) {
        ExamSession session = sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new BusinessException(
                        ReservationErrorCode.RSV_SESSION_NOT_FOUND,
                        "未找到该场次",
                        404));

        List<SessionReservation> bookings = reservationRepository
                .findBySessionIdOrderByReservedAtDesc(sessionId)
                .stream()
                .filter(r -> r.getStatus() == ReservationStatus.BOOKED)
                .collect(Collectors.toList());

        for (SessionReservation booking : bookings) {
            booking.setStatus(ReservationStatus.CANCELLED);
            reservationRepository.save(booking);
        }

        session.setStatus(SessionStatus.CLOSED);
        sessionRepository.save(session);

        log.info("Admin offline session [ID: {}], cancelled {} reservations", sessionId, bookings.size());
        return new OfflineSessionResponse(sessionId, bookings.size());
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> listAdminSessions(Long examId, SessionStatus status) {
        List<ExamSession> sessions;
        if (examId != null && status != null) {
            sessions = sessionRepository.findByExamIdAndStatusOrderByStartTimeAsc(examId, status);
        } else if (examId != null) {
            sessions = sessionRepository.findByExamIdOrderByStartTimeAsc(examId);
        } else if (status != null) {
            sessions = sessionRepository.findByStatusOrderByStartTimeAsc(status);
        } else {
            sessions = sessionRepository.findAllByOrderByStartTimeAsc();
        }
        return sessions.stream()
                .map(s -> SessionResponse.from(s,
                        (int) reservationRepository.countBySessionIdAndStatus(s.getId(), ReservationStatus.BOOKED),
                        (int) reservationRepository.countBySessionIdAndStatusAndStartedAtIsNotNull(
                                s.getId(), ReservationStatus.BOOKED)))
                .collect(Collectors.toList());
    }

    private SessionResponse toUserSessionResponse(ExamSession s) {
        return SessionResponse.from(s,
                (int) reservationRepository.countBySessionIdAndStatus(s.getId(), ReservationStatus.BOOKED),
                (int) reservationRepository.countBySessionIdAndStatusAndStartedAtIsNotNull(
                        s.getId(), ReservationStatus.BOOKED));
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> listSessionReservations(Long sessionId) {
        if (!sessionRepository.existsById(sessionId)) {
            throw new BusinessException(
                    ReservationErrorCode.RSV_SESSION_NOT_FOUND,
                    "未找到该场次",
                    404);
        }
        return reservationRepository.findBySessionIdOrderByReservedAtDesc(sessionId).stream()
                .map(ReservationResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> listOpenSessionsForUser() {
        List<ExamSession> sessions = sessionRepository.findByStatusOrderByStartTimeAsc(SessionStatus.OPEN);
        LocalDateTime now = LocalDateTime.now();
        return sessions.stream()
                .filter(s -> s.getStartTime().isAfter(now))
                .sorted(Comparator.comparing(ExamSession::getStartTime))
                .map(this::toUserSessionResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReservationResponse bookSession(Long sessionId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("未找到当前用户"));

        ExamSession session = sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new BusinessException(
                        ReservationErrorCode.RSV_SESSION_NOT_FOUND,
                        "未找到该场次",
                        404));

        if (session.getStatus() != SessionStatus.OPEN) {
            throw new BusinessException(
                    ReservationErrorCode.RSV_SESSION_NOT_OPEN,
                    "该场次当前未开放预约");
        }

        LocalDateTime now = LocalDateTime.now();
        long minutesUntilStart = ChronoUnit.MINUTES.between(now, session.getStartTime());
        if (minutesUntilStart < MIN_LEAD_MINUTES) {
            throw new BusinessException(
                    ReservationErrorCode.RSV_BELOW_LEAD_TIME,
                    "距离场次开始不足 " + MIN_LEAD_MINUTES + " 分钟，已无法预约");
        }

        if (reservationRepository.existsBySessionIdAndUserIdAndStatus(
                sessionId, user.getId(), ReservationStatus.BOOKED)) {
            throw new BusinessException(
                    ReservationErrorCode.RSV_DUPLICATE_BOOKING,
                    "您已经预约过该场次，请勿重复预约");
        }

        long bookedCount = reservationRepository.countBySessionIdAndStatus(sessionId, ReservationStatus.BOOKED);
        if (bookedCount >= session.getCapacity()) {
            throw new BusinessException(
                    ReservationErrorCode.RSV_CAPACITY_FULL,
                    "该场次名额已满");
        }

        SessionReservation reservation = new SessionReservation();
        reservation.setSession(session);
        reservation.setUser(user);
        reservation.setReservedAt(now);
        reservation.setStatus(ReservationStatus.BOOKED);

        try {
            SessionReservation saved = reservationRepository.save(reservation);
            reservationRepository.flush();
            log.info("User [{}] booked session [ID: {}]", username, sessionId);
            return ReservationResponse.from(saved);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Concurrent booking conflict for session [{}], user [{}]", sessionId, username);
            throw new BusinessException(
                    ReservationErrorCode.RSV_CAPACITY_FULL,
                    "预约人数较多，请稍后再试");
        }
    }

    @Transactional
    public void cancelReservation(Long reservationId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("未找到当前用户"));

        SessionReservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException(
                        ReservationErrorCode.RSV_NO_RESERVATION,
                        "未找到该预约记录",
                        404));

        if (!reservation.getUser().getId().equals(user.getId())) {
            throw new BusinessException(
                    ReservationErrorCode.RSV_NO_RESERVATION,
                    "无权操作他人的预约",
                    403);
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new BusinessException(
                    ReservationErrorCode.RSV_RESERVATION_CANCELLED,
                    "该预约已取消");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);
        log.info("User [{}] cancelled reservation [ID: {}] for session [ID: {}]",
                username, reservationId, reservation.getSession().getId());
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> listMyReservations(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("未找到当前用户"));

        return reservationRepository
                .findByUserIdAndStatusOrderByReservedAtDesc(user.getId(), ReservationStatus.BOOKED)
                .stream()
                .map(ReservationResponse::from)
                .collect(Collectors.toList());
    }
}
