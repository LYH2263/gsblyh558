package com.exam.system.service;

import com.exam.system.dto.ExamEntryResponse;
import com.exam.system.dto.ScreenSwitchReportRequest;
import com.exam.system.dto.ScreenSwitchReportResponse;
import com.exam.system.dto.SessionExamSubmission;
import com.exam.system.entity.Exam;
import com.exam.system.entity.ExamResult;
import com.exam.system.entity.ExamSession;
import com.exam.system.entity.ReservationStatus;
import com.exam.system.entity.SessionReservation;
import com.exam.system.entity.User;
import com.exam.system.exception.BusinessException;
import com.exam.system.exception.ReservationErrorCode;
import com.exam.system.exception.ResourceNotFoundException;
import com.exam.system.repository.ExamResultRepository;
import com.exam.system.repository.ExamSessionRepository;
import com.exam.system.repository.SessionReservationRepository;
import com.exam.system.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
public class ExamTakingService {

    private static final int DEFAULT_LATE_GRACE_MINUTES = 10;
    private static final int DEFAULT_SCREEN_SWITCH_LIMIT = 3;

    @Autowired
    private ExamSessionRepository sessionRepository;

    @Autowired
    private SessionReservationRepository reservationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExamResultRepository examResultRepository;

    @Autowired
    private ExamGradingService gradingService;

    @Transactional
    public ExamEntryResponse enterSession(Long sessionId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("未找到当前用户"));

        ExamSession session = sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new BusinessException(
                        ReservationErrorCode.RSV_SESSION_NOT_FOUND,
                        "未找到该场次",
                        404));

        LocalDateTime now = LocalDateTime.now();

        SessionReservation reservation = reservationRepository
                .findBySessionIdAndUserIdAndStatus(sessionId, user.getId(), ReservationStatus.BOOKED)
                .orElse(null);

        if (reservation == null) {
            throw new BusinessException(
                    ReservationErrorCode.RSV_NO_RESERVATION,
                    "您没有预约该场次，无法进入考试",
                    403);
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new BusinessException(
                    ReservationErrorCode.RSV_RESERVATION_CANCELLED,
                    "您的预约已取消，无法进入考试",
                    403);
        }

        if (now.isBefore(session.getStartTime())) {
            throw new BusinessException(
                    ReservationErrorCode.RSV_SESSION_NOT_STARTED,
                    "该场次尚未开始，无法进入考试");
        }

        LocalDateTime sessionEndTime = computeDeadline(session);
        if (!now.isBefore(sessionEndTime)) {
            throw new BusinessException(
                    ReservationErrorCode.RSV_SESSION_ENDED,
                    "该场次已结束，无法进入考试");
        }

        LocalDateTime lateEntryDeadline = session.getStartTime()
                .plusMinutes(resolveLateGraceMinutes(session));
        if (now.isAfter(lateEntryDeadline)) {
            throw new BusinessException(
                    ReservationErrorCode.RSV_LATE_GRACE_EXCEEDED,
                    "已超过迟到入场宽限时间，无法进入考试");
        }

        if (reservation.getStartedAt() == null) {
            reservation.setStartedAt(now);
        }
        if (reservation.getScreenSwitchCount() == null) {
            reservation.setScreenSwitchCount(0);
        }
        if (reservation.getForceSubmitted() == null) {
            reservation.setForceSubmitted(false);
        }
        reservationRepository.save(reservation);

        ExamEntryResponse response = new ExamEntryResponse();
        response.setReservationId(reservation.getId());
        response.setSessionId(session.getId());
        response.setExamId(session.getExam().getId());
        response.setExamTitle(session.getExam().getTitle());
        response.setSessionStartTime(session.getStartTime());
        response.setDurationMinutes(session.getDurationMinutes());
        response.setLateGraceMinutes(resolveLateGraceMinutes(session));
        response.setScreenSwitchLimit(resolveScreenSwitchLimit(session));
        response.setScreenSwitchCount(reservation.getScreenSwitchCount());
        response.setDeadline(computeDeadline(session));
        response.setServerTime(System.currentTimeMillis());
        return response;
    }

    @Transactional
    public ExamResult submitSessionExam(SessionExamSubmission submission, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("未找到当前用户"));

        SessionReservation reservation = reservationRepository.findById(submission.getReservationId())
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
                    "该预约已取消，无法提交",
                    403);
        }

        if (Boolean.TRUE.equals(reservation.getForceSubmitted())) {
            throw new BusinessException(
                    ReservationErrorCode.RSV_SCREEN_SWITCH_FORCED,
                    "因切屏次数已达上限，本场考试已被强制交卷",
                    403);
        }

        ExamSession session = reservation.getSession();
        if (!session.getId().equals(submission.getSessionId())) {
            throw new BusinessException(
                    ReservationErrorCode.RSV_SESSION_MISMATCH,
                    "预约与场次信息不匹配");
        }

        Exam exam = session.getExam();
        if (!exam.getId().equals(reservation.getSession().getExam().getId())) {
            throw new BusinessException(
                    ReservationErrorCode.RSV_EXAM_MISMATCH,
                    "预约与考试信息不匹配");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = computeDeadline(session);
        boolean timeout = now.isAfter(deadline);

        if (now.isBefore(session.getStartTime())) {
            throw new BusinessException(
                    ReservationErrorCode.RSV_SESSION_NOT_STARTED,
                    "该场次尚未开始，无法提交");
        }

        if (reservation.getStartedAt() == null) {
            throw new BusinessException(
                    ReservationErrorCode.RSV_NO_RESERVATION,
                    "您尚未进入考试，无法提交",
                    403);
        }

        int score = gradingService.gradeExam(exam, user, submission.getAnswers());

        ExamResult saved = buildAndSaveResult(session, reservation, user,
                submission.getAnswers(), now, timeout, false);

        log.info("User [{}] submitted session [ID: {}], reservation [ID: {}], score={}, timeout={}",
                username, session.getId(), reservation.getId(), score, timeout);
        return saved;
    }

    @Transactional
    public ScreenSwitchReportResponse reportScreenSwitch(ScreenSwitchReportRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("未找到当前用户"));

        SessionReservation reservation = reservationRepository.findByIdForUpdate(request.getReservationId())
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
        if (!reservation.getSession().getId().equals(request.getSessionId())) {
            throw new BusinessException(
                    ReservationErrorCode.RSV_SESSION_MISMATCH,
                    "预约与场次信息不匹配");
        }
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new BusinessException(
                    ReservationErrorCode.RSV_RESERVATION_CANCELLED,
                    "该预约已取消",
                    403);
        }
        if (Boolean.TRUE.equals(reservation.getForceSubmitted())) {
            throw new BusinessException(
                    ReservationErrorCode.RSV_SCREEN_SWITCH_FORCED,
                    "本场考试已被强制交卷",
                    403);
        }

        ExamSession session = reservation.getSession();
        LocalDateTime now = LocalDateTime.now();

        int currentCount = reservation.getScreenSwitchCount() == null ? 0 : reservation.getScreenSwitchCount();
        currentCount += 1;
        reservation.setScreenSwitchCount(currentCount);

        int limit = resolveScreenSwitchLimit(session);

        if (currentCount >= limit) {
            // Force submit with current answers
            ExamResult result = buildAndSaveResult(session, reservation, user,
                    request.getAnswers(), now, false, true);
            reservation.setForceSubmitted(true);
            reservationRepository.save(reservation);
            log.warn("User [{}] reached screen switch limit ({}) on session [ID: {}], force submitted. score={}",
                    username, limit, session.getId(), result.getScore());
            return ScreenSwitchReportResponse.forced(currentCount, limit, result);
        }

        reservationRepository.save(reservation);
        return ScreenSwitchReportResponse.of(currentCount, limit);
    }

    private ExamResult buildAndSaveResult(ExamSession session, SessionReservation reservation, User user,
                                         java.util.Map<Long, String> answers, LocalDateTime now,
                                         boolean timeout, boolean screenSwitchForced) {
        boolean isTimeout = timeout || now.isAfter(computeDeadline(session));
        int score = gradingService.gradeExam(session.getExam(), user, answers);

        ExamResult result = new ExamResult();
        result.setUser(user);
        result.setExam(session.getExam());
        result.setSession(session);
        result.setReservation(reservation);
        result.setScore(score);
        result.setTimeout(isTimeout);
        result.setScreenSwitchForced(screenSwitchForced);
        return examResultRepository.save(result);
    }

    private int resolveLateGraceMinutes(ExamSession session) {
        if (session.getLateGraceMinutes() == null) {
            return DEFAULT_LATE_GRACE_MINUTES;
        }
        return session.getLateGraceMinutes();
    }

    private int resolveScreenSwitchLimit(ExamSession session) {
        if (session.getScreenSwitchLimit() == null || session.getScreenSwitchLimit() < 1) {
            return DEFAULT_SCREEN_SWITCH_LIMIT;
        }
        return session.getScreenSwitchLimit();
    }

    private LocalDateTime computeDeadline(ExamSession session) {
        return session.getStartTime().plusMinutes(session.getDurationMinutes());
    }
}
