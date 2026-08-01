package com.exam.system.service;

import com.exam.system.dto.ReservationExamAccessResponse;
import com.exam.system.dto.ReservationResponse;
import com.exam.system.dto.SessionAnalyticsResponse;
import com.exam.system.dto.SessionRequest;
import com.exam.system.dto.SessionResponse;
import com.exam.system.dto.TabSwitchResponse;
import com.exam.system.entity.*;
import com.exam.system.exception.BusinessException;
import com.exam.system.repository.ExamRecordRepository;
import com.exam.system.repository.ExamRepository;
import com.exam.system.repository.ExamResultRepository;
import com.exam.system.repository.ExamSessionRepository;
import com.exam.system.repository.SessionReservationRepository;
import com.exam.system.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class SessionService {
    private static final int MIN_ADVANCE_MINUTES = 30;
    private static final int DEFAULT_LATE_GRACE_MINUTES = 10;
    private static final int DEFAULT_TAB_SWITCH_LIMIT = 3;

    @Autowired
    private ExamSessionRepository sessionRepository;

    @Autowired
    private SessionReservationRepository reservationRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExamRecordRepository examRecordRepository;

    @Autowired
    private ExamResultRepository examResultRepository;

    @Autowired
    private ExamSubmissionService examSubmissionService;

    @Transactional(readOnly = true)
    public List<SessionResponse> getAvailableSessions() {
        return sessionRepository.findByStatusAndStartTimeAfterOrderByStartTimeAsc(SessionStatus.OPEN, LocalDateTime.now())
                .stream()
                .map(this::toSessionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> getAdminSessions(Long examId, SessionStatus status) {
        List<ExamSession> sessions;
        if (examId != null && status != null) {
            sessions = sessionRepository.findByExamIdAndStatusOrderByStartTimeDesc(examId, status);
        } else if (examId != null) {
            sessions = sessionRepository.findByExamIdOrderByStartTimeDesc(examId);
        } else if (status != null) {
            sessions = sessionRepository.findByStatusOrderByStartTimeDesc(status);
        } else {
            sessions = sessionRepository.findAllByOrderByStartTimeDesc();
        }
        return sessions.stream().map(this::toSessionResponse).toList();
    }

    @Transactional
    public SessionResponse createSession(SessionRequest request) {
        Exam exam = getExam(request.examId());
        validateStartTime(request.startTime());

        ExamSession session = new ExamSession();
        session.setExam(exam);
        applyRequest(session, request);
        ExamSession saved = sessionRepository.save(session);
        log.info("Exam session created with ID: {} for exam ID: {}", saved.getId(), exam.getId());
        return toSessionResponse(saved);
    }

    @Transactional
    public SessionResponse updateSession(Long id, SessionRequest request) {
        ExamSession session = sessionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "RSV_SESSION_NOT_FOUND", "场次不存在"));
        Exam exam = getExam(request.examId());
        validateStartTime(request.startTime());

        long bookedCount = reservationRepository.countBySessionAndStatus(session, ReservationStatus.BOOKED);
        if (request.capacity() < bookedCount) {
            throw new BusinessException("RSV_CAPACITY_BELOW_BOOKED", "最大预约人数不能小于已预约人数");
        }

        session.setExam(exam);
        applyRequest(session, request);
        ExamSession saved = sessionRepository.save(session);
        log.info("Exam session updated with ID: {}", saved.getId());
        return toSessionResponse(saved);
    }

    @Transactional
    public SessionResponse closeSession(Long id) {
        ExamSession session = sessionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "RSV_SESSION_NOT_FOUND", "场次不存在"));
        session.setStatus(SessionStatus.CLOSED);
        ExamSession saved = sessionRepository.save(session);
        long bookedCount = reservationRepository.countBySessionAndStatus(session, ReservationStatus.BOOKED);
        log.info("Exam session closed with ID: {}, booked count: {}", saved.getId(), bookedCount);
        return toSessionResponse(saved, bookedCount);
    }

    @Transactional
    public ReservationResponse bookSession(String username, Long sessionId) {
        ExamSession session = sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "RSV_SESSION_NOT_FOUND", "场次不存在"));
        User user = getUser(username);
        LocalDateTime now = LocalDateTime.now();

        if (session.getStatus() != SessionStatus.OPEN) {
            throw new BusinessException("RSV_SESSION_NOT_AVAILABLE", "该场次当前不可预约");
        }

        long minutesUntilStart = Duration.between(now, session.getStartTime()).toMinutes();
        if (minutesUntilStart < MIN_ADVANCE_MINUTES) {
            throw new BusinessException("RSV_BELOW_MIN_ADVANCE", "距场次开始不足30分钟，无法预约");
        }

        if (reservationRepository.existsBySessionAndUserAndStatus(session, user, ReservationStatus.BOOKED)) {
            throw new BusinessException("RSV_DUPLICATE_BOOKING", "您已预约该场次，不能重复预约");
        }

        long bookedCount = reservationRepository.countBySessionAndStatus(session, ReservationStatus.BOOKED);
        if (bookedCount >= session.getCapacity()) {
            throw new BusinessException("RSV_CAPACITY_FULL", "该场次名额已满");
        }

        SessionReservation reservation = new SessionReservation();
        reservation.setSession(session);
        reservation.setUser(user);
        reservation.setStatus(ReservationStatus.BOOKED);
        reservation.setReservedAt(now);
        SessionReservation saved = reservationRepository.save(reservation);
        log.info("User [{}] booked session [ID: {}]", username, sessionId);
        return toReservationResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getMyReservations(String username) {
        User user = getUser(username);
        return reservationRepository.findByUserAndStatusOrderByReservedAtDesc(user, ReservationStatus.BOOKED)
                .stream()
                .map(this::toReservationResponse)
                .toList();
    }

    @Transactional
    public void cancelReservation(String username, Long reservationId) {
        SessionReservation reservation = reservationRepository.findByIdAndStatus(reservationId, ReservationStatus.BOOKED)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "RSV_RESERVATION_NOT_FOUND", "预约记录不存在或已取消"));
        User user = getUser(username);
        if (!reservation.getUser().getId().equals(user.getId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "RSV_NOT_OWNER", "只能取消自己的预约");
        }

        sessionRepository.findByIdForUpdate(reservation.getSession().getId());
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);
        log.info("User [{}] cancelled reservation [ID: {}]", username, reservationId);
    }

    @Transactional
    public ReservationExamAccessResponse getExamAccess(String username, Long reservationId) {
        User user = getUser(username);
        SessionReservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "RSV_NO_RESERVATION", "未找到该预约记录"));
        if (!reservation.getUser().getId().equals(user.getId())) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "RSV_NO_RESERVATION", "未找到该预约记录");
        }
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new BusinessException("RSV_RESERVATION_CANCELLED", "该预约已取消，不能进入考试");
        }

        ExamSession session = reservation.getSession();
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(session.getStartTime())) {
            throw new BusinessException("RSV_SESSION_NOT_STARTED", "考试尚未开始，请在开考后进入");
        }

        LocalDateTime entryDeadline = session.getStartTime().plusMinutes(session.getLateGraceMinutes());
        LocalDateTime examDeadline = session.getStartTime().plusMinutes(session.getDurationMinutes());
        if (!now.isBefore(examDeadline)) {
            throw new BusinessException("RSV_SESSION_ENDED", "考试已结束，不能进入");
        }
        if (session.getStatus() != SessionStatus.OPEN) {
            throw new BusinessException("RSV_SESSION_NOT_AVAILABLE", "该场次当前不可进入");
        }
        if (now.isAfter(entryDeadline)) {
            throw new BusinessException("RSV_LATE_GRACE_EXCEEDED", "已超过迟到入场宽限时间，不能进入考试");
        }

        ensureStartedRecord(user, session, reservation, now);

        int tabSwitchCount = examRecordRepository.findByReservation(reservation)
                .map(r -> r.getTabSwitchCount() == null ? 0 : r.getTabSwitchCount())
                .orElse(0);

        long remainingMinutes = Duration.between(now, examDeadline).toMinutes();
        return new ReservationExamAccessResponse(
                reservation.getId(),
                session.getId(),
                session.getExam().getId(),
                session.getExam().getTitle(),
                session.getStartTime(),
                session.getDurationMinutes(),
                session.getLateGraceMinutes(),
                session.getTabSwitchLimit(),
                tabSwitchCount,
                (int) Math.max(0, remainingMinutes),
                now,
                session.getStatus()
        );
    }

    @Transactional
    public TabSwitchResponse reportTabSwitch(String username, Long reservationId) {
        User user = getUser(username);
        SessionReservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "RSV_NO_RESERVATION", "未找到该预约记录"));
        if (!reservation.getUser().getId().equals(user.getId())) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "RSV_NO_RESERVATION", "未找到该预约记录");
        }
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new BusinessException("RSV_RESERVATION_CANCELLED", "该预约已取消");
        }

        ExamSession session = sessionRepository.findByIdForUpdate(reservation.getSession().getId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "RSV_SESSION_NOT_FOUND", "场次不存在"));

        ExamRecord record = examRecordRepository.findByReservation(reservation)
                .orElseGet(() -> {
                    ExamRecord r = new ExamRecord();
                    r.setUser(user);
                    r.setExam(session.getExam());
                    r.setSession(session);
                    r.setReservation(reservation);
                    r.setStartTime(LocalDateTime.now());
                    r.setTabSwitchCount(0);
                    return examRecordRepository.save(r);
                });

        int newCount = (record.getTabSwitchCount() == null ? 0 : record.getTabSwitchCount()) + 1;
        record.setTabSwitchCount(newCount);
        examRecordRepository.save(record);
        log.info("User [{}] tab-switch reported for reservation [ID: {}], count: {}", username, reservationId, newCount);

        int limit = session.getTabSwitchLimit() == null ? DEFAULT_TAB_SWITCH_LIMIT : session.getTabSwitchLimit();
        boolean exceeded = newCount >= limit;
        if (exceeded) {
            log.warn("Tab-switch threshold reached for reservation [ID: {}], force submit required", reservationId);
        }
        return new TabSwitchResponse(newCount, limit, exceeded);
    }

    @Transactional
    public ExamResult submitReservationExam(String username, Long pathExamId, Long reservationId,
                                           Long sessionId, ForcedSubmitReason forcedSubmitReason,
                                           Map<Long, String> answers) {
        User user = getUser(username);
        SessionReservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "RSV_NO_RESERVATION", "未找到该预约记录"));
        if (!reservation.getUser().getId().equals(user.getId())) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "RSV_NO_RESERVATION", "未找到该预约记录");
        }
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new BusinessException("RSV_RESERVATION_CANCELLED", "预约已取消，不能提交试卷");
        }

        ExamSession session = reservation.getSession();
        if (sessionId != null && !session.getId().equals(sessionId)) {
            throw new BusinessException("RSV_SESSION_MISMATCH", "预约场次与提交场次不一致");
        }
        if (!session.getExam().getId().equals(pathExamId)) {
            throw new BusinessException("RSV_EXAM_MISMATCH", "预约考试与提交考试不一致");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime examDeadline = session.getStartTime().plusMinutes(session.getDurationMinutes());
        boolean timedOut = !now.isBefore(examDeadline);

        ExamRecord record = examRecordRepository.findByReservation(reservation).orElse(null);
        int tabSwitchCount = record != null && record.getTabSwitchCount() != null ? record.getTabSwitchCount() : 0;
        int limit = session.getTabSwitchLimit() == null ? DEFAULT_TAB_SWITCH_LIMIT : session.getTabSwitchLimit();
        boolean forcedByTabSwitch = tabSwitchCount >= limit;

        ForcedSubmitReason reason = forcedSubmitReason;
        if (reason == null) {
            if (forcedByTabSwitch) {
                reason = ForcedSubmitReason.TAB_SWITCH;
            } else if (timedOut) {
                reason = ForcedSubmitReason.TIMEOUT;
            }
        }

        ExamResult result = examSubmissionService.submitForReservation(
                user, session.getExam(), session, reservation, answers, timedOut, reason);
        if (record != null) {
            record.setSubmitTime(now);
            record.setScore(result.getScore());
            examRecordRepository.save(record);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<SessionAnalyticsResponse> getSessionAnalytics(SessionStatus status) {
        List<ExamSession> sessions = status == null
                ? sessionRepository.findAllByOrderByStartTimeDesc()
                : sessionRepository.findByStatusOrderByStartTimeDesc(status);
        return sessions.stream().map(this::toAnalytics).toList();
    }

    private SessionAnalyticsResponse toAnalytics(ExamSession session) {
        long reservationCount = reservationRepository.countBySessionAndStatus(session, ReservationStatus.BOOKED);
        List<ExamRecord> records = examRecordRepository.findBySession(session);
        long startedCount = records.size();

        List<ExamResult> results = examResultRepository.findBySession(session);
        long completedOnTime = results.stream()
                .filter(r -> !Boolean.TRUE.equals(r.getTimedOut()))
                .filter(r -> r.getForcedSubmitReason() != ForcedSubmitReason.TAB_SWITCH)
                .count();
        long timedOutCount = results.stream()
                .filter(r -> Boolean.TRUE.equals(r.getTimedOut()))
                .count();
        long forcedByTabCount = results.stream()
                .filter(r -> r.getForcedSubmitReason() == ForcedSubmitReason.TAB_SWITCH)
                .count();

        double avgMinutes = records.stream()
                .filter(r -> r.getSubmitTime() != null && r.getStartTime() != null)
                .mapToLong(r -> Duration.between(r.getStartTime(), r.getSubmitTime()).toMinutes())
                .average().orElse(0);

        return new SessionAnalyticsResponse(
                session.getId(),
                session.getExam().getTitle(),
                session.getStartTime(),
                session.getDurationMinutes(),
                session.getStatus().name(),
                (int) reservationCount,
                (int) startedCount,
                (int) completedOnTime,
                (int) timedOutCount,
                (int) forcedByTabCount,
                (int) Math.round(avgMinutes)
        );
    }

    private void ensureStartedRecord(User user, ExamSession session, SessionReservation reservation, LocalDateTime now) {
        if (examRecordRepository.existsByReservation(reservation)) {
            return;
        }
        ExamRecord record = new ExamRecord();
        record.setUser(user);
        record.setExam(session.getExam());
        record.setSession(session);
        record.setReservation(reservation);
        record.setStartTime(now);
        examRecordRepository.save(record);
    }

    private void applyRequest(ExamSession session, SessionRequest request) {
        session.setStartTime(request.startTime());
        session.setDurationMinutes(request.durationMinutes());
        session.setCapacity(request.capacity());
        session.setLateGraceMinutes(request.lateGraceMinutes() == null ? DEFAULT_LATE_GRACE_MINUTES : request.lateGraceMinutes());
        session.setTabSwitchLimit(request.tabSwitchLimit() == null ? DEFAULT_TAB_SWITCH_LIMIT : request.tabSwitchLimit());
        session.setStatus(request.status());
    }

    private void validateStartTime(LocalDateTime startTime) {
        if (startTime.isBefore(LocalDateTime.now())) {
            throw new BusinessException("RSV_START_TIME_IN_PAST", "场次开始时间不能早于当前时间");
        }
    }

    private Exam getExam(Long examId) {
        return examRepository.findById(examId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "RSV_EXAM_NOT_FOUND", "关联考试不存在"));
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "RSV_USER_NOT_FOUND", "用户不存在"));
    }

    private SessionResponse toSessionResponse(ExamSession session) {
        long bookedCount = reservationRepository.countBySessionAndStatus(session, ReservationStatus.BOOKED);
        return toSessionResponse(session, bookedCount);
    }

    private SessionResponse toSessionResponse(ExamSession session, long bookedCount) {
        long startedCount = examRecordRepository.countBySession(session);
        return new SessionResponse(
                session.getId(),
                session.getExam().getId(),
                session.getExam().getTitle(),
                session.getStartTime(),
                session.getDurationMinutes(),
                session.getCapacity(),
                (int) bookedCount,
                session.getCapacity() - (int) bookedCount,
                (int) startedCount,
                session.getLateGraceMinutes(),
                session.getTabSwitchLimit(),
                session.getStatus(),
                session.getCreatedAt()
        );
    }

    private ReservationResponse toReservationResponse(SessionReservation reservation) {
        ExamSession session = reservation.getSession();
        return new ReservationResponse(
                reservation.getId(),
                session.getId(),
                session.getExam().getId(),
                session.getExam().getTitle(),
                session.getStartTime(),
                session.getDurationMinutes(),
                session.getCapacity(),
                session.getStatus(),
                reservation.getStatus(),
                reservation.getReservedAt()
        );
    }
}
