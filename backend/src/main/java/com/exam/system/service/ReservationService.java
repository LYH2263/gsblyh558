package com.exam.system.service;

import com.exam.system.entity.ExamSession;
import com.exam.system.entity.ReservationStatus;
import com.exam.system.entity.SessionReservation;
import com.exam.system.entity.SessionStatus;
import com.exam.system.entity.User;
import com.exam.system.config.ExamProctorProperties;
import com.exam.system.exception.ReservationErrorCode;
import com.exam.system.exception.ReservationException;
import com.exam.system.exception.ResourceNotFoundException;
import com.exam.system.payload.response.ReservationResponse;
import com.exam.system.payload.response.SessionAccessResponse;
import com.exam.system.payload.response.SessionResponse;
import com.exam.system.payload.response.SwitchScreenResponse;
import com.exam.system.repository.ExamSessionRepository;
import com.exam.system.repository.SessionReservationRepository;
import com.exam.system.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户端场次浏览与预约业务。
 * 第 3 条硬门槛：用户只能预约「当前时间距离场次开始时间 ≥ 30 分钟」的场次。
 */
@Service
@Slf4j
public class ReservationService {

    /** 预约时间门槛（分钟）。低于该门槛一律拒绝。 */
    private static final int BOOKING_THRESHOLD_MINUTES = 30;

    @Autowired
    private ExamSessionRepository examSessionRepository;

    @Autowired
    private SessionReservationRepository sessionReservationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExamProctorProperties proctorProperties;

    @Autowired
    private ExamService examService;

    /**
     * 浏览可预约场次（仅 OPEN 状态），附剩余名额与当前用户是否已预约。
     */
    @Transactional(readOnly = true)
    public List<SessionResponse> listOpenSessions(String username) {
        User user = getUser(username);
        return examSessionRepository.findByStatus(SessionStatus.OPEN).stream()
                .map(session -> toSessionResponse(session, user.getId()))
                .collect(Collectors.toList());
    }

    /**
     * 预约场次。使用悲观写锁读取场次，配合名额校验防止并发超卖。
     */
    @Transactional
    public ReservationResponse book(Long sessionId, String username) {
        User user = getUser(username);

        // 悲观写锁：并发预约在此串行化，杜绝超卖
        ExamSession session = examSessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new ReservationException(ReservationErrorCode.RSV_SESSION_NOT_FOUND));

        if (session.getStatus() != SessionStatus.OPEN) {
            throw new ReservationException(ReservationErrorCode.RSV_SESSION_NOT_OPEN);
        }

        // 时间门槛校验
        long minutesToStart = Duration.between(LocalDateTime.now(), session.getStartTime()).toMinutes();
        if (minutesToStart < BOOKING_THRESHOLD_MINUTES) {
            throw new ReservationException(ReservationErrorCode.RSV_BELOW_TIME_THRESHOLD);
        }

        // 重复预约校验
        sessionReservationRepository
                .findBySessionIdAndUserIdAndStatus(sessionId, user.getId(), ReservationStatus.BOOKED)
                .ifPresent(r -> {
                    throw new ReservationException(ReservationErrorCode.RSV_ALREADY_BOOKED);
                });

        // 名额校验（锁内计数，安全）
        long booked = sessionReservationRepository.countBySessionIdAndStatus(sessionId, ReservationStatus.BOOKED);
        if (booked >= session.getCapacity()) {
            throw new ReservationException(ReservationErrorCode.RSV_SESSION_FULL);
        }

        SessionReservation reservation = new SessionReservation();
        reservation.setSession(session);
        reservation.setUser(user);
        reservation.setStatus(ReservationStatus.BOOKED);
        SessionReservation saved = sessionReservationRepository.save(reservation);
        log.info("User [{}] booked session [ID: {}], now {} / {} seats used",
                username, sessionId, booked + 1, session.getCapacity());
        return toReservationResponse(saved);
    }

    /**
     * 取消预约，名额随之实时释放（改为 CANCELLED 状态）。
     */
    @Transactional
    public ReservationResponse cancel(Long reservationId, String username) {
        User user = getUser(username);
        SessionReservation reservation = sessionReservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationException(ReservationErrorCode.RSV_RESERVATION_NOT_FOUND));

        if (!reservation.getUser().getId().equals(user.getId())
                || reservation.getStatus() != ReservationStatus.BOOKED) {
            throw new ReservationException(ReservationErrorCode.RSV_RESERVATION_NOT_FOUND);
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancelledAt(LocalDateTime.now());
        SessionReservation saved = sessionReservationRepository.save(reservation);
        log.info("User [{}] cancelled reservation [ID: {}] on session [ID: {}]",
                username, reservationId, reservation.getSession().getId());
        return toReservationResponse(saved);
    }

    /**
     * 我的预约（含已取消，用于历史查看，按预约时间倒序）。
     */
    @Transactional(readOnly = true)
    public List<ReservationResponse> listMyReservations(String username) {
        User user = getUser(username);
        return sessionReservationRepository.findByUserIdOrderByReservedAtDesc(user.getId()).stream()
                .map(this::toReservationResponse)
                .collect(Collectors.toList());
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("未找到该用户"));
    }

    /**
     * 开考准入校验。只有「已预约该场次且当前时间落在有效作答窗口内」才允许进入答题页。
     * 四种失败分别返回独立 RSV_ 错误码，供前端展示不同文案：
     * - 未预约          -> RSV_ACCESS_NOT_BOOKED
     * - 预约已取消      -> RSV_ACCESS_CANCELLED
     * - 场次尚未开始    -> RSV_ACCESS_NOT_STARTED
     * - 场次已结束      -> RSV_ACCESS_ENDED
     *
     * 可进入窗口：[startTime, startTime + lateEntryMinutes]（迟到宽限量之内仍可进入）。
     * 作答截止：startTime + durationMinutes。超过截止亦视为已结束。
     * 进入成功时记录 startedAt（首次），用于统计已开考人数，并返回剩余作答秒数。
     */
    @Transactional
    public SessionAccessResponse checkAccess(Long sessionId, String username) {
        User user = getUser(username);

        ExamSession session = examSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ReservationException(ReservationErrorCode.RSV_SESSION_NOT_FOUND));

        // 是否存在任意预约（无论状态），用于区分「未预约」与「已取消」
        SessionReservation booked = sessionReservationRepository
                .findBySessionIdAndUserIdAndStatus(sessionId, user.getId(), ReservationStatus.BOOKED)
                .orElse(null);
        if (booked == null) {
            boolean everReserved = sessionReservationRepository
                    .findByUserIdOrderByReservedAtDesc(user.getId()).stream()
                    .anyMatch(r -> r.getSession().getId().equals(sessionId));
            if (everReserved) {
                throw new ReservationException(ReservationErrorCode.RSV_ACCESS_CANCELLED);
            }
            throw new ReservationException(ReservationErrorCode.RSV_ACCESS_NOT_BOOKED);
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = session.getStartTime();
        LocalDateTime entryDeadline = startTime.plusMinutes(session.getLateEntryMinutes());
        LocalDateTime answerDeadline = startTime.plusMinutes(session.getDurationMinutes());

        if (now.isBefore(startTime)) {
            throw new ReservationException(ReservationErrorCode.RSV_ACCESS_NOT_STARTED);
        }
        // 超过可进入窗口或超过作答截止，一律视为已结束
        if (now.isAfter(entryDeadline) || !now.isBefore(answerDeadline)) {
            throw new ReservationException(ReservationErrorCode.RSV_ACCESS_ENDED);
        }

        // 记录首次开考时间（用于已开考人数统计）
        if (booked.getStartedAt() == null) {
            booked.setStartedAt(now);
            sessionReservationRepository.save(booked);
        }

        long remainingSeconds = Duration.between(now, answerDeadline).getSeconds();
        SessionAccessResponse response = new SessionAccessResponse();
        response.setSessionId(session.getId());
        response.setReservationId(booked.getId());
        response.setExamId(session.getExam().getId());
        response.setExamTitle(session.getExam().getTitle());
        response.setStartTime(startTime);
        response.setDurationMinutes(session.getDurationMinutes());
        response.setRemainingSeconds(Math.max(0, remainingSeconds));
        response.setRemainingMinutes((int) (Math.max(0, remainingSeconds) / 60));
        log.info("User [{}] granted access to session [ID: {}], remaining {}s", username, sessionId, remainingSeconds);
        return response;
    }

    /**
     * 切屏上报：累计次数落库并与预约记录关联。达到可配置阈值时拒绝继续作答并强制交卷。
     * 强制交卷按已作答内容计分，结果以 FORCED 标记原因。
     */
    @Transactional(noRollbackFor = ReservationException.class)
    public SwitchScreenResponse reportSwitchScreen(Long reservationId, String username, Map<Long, String> answers) {
        User user = getUser(username);
        SessionReservation reservation = sessionReservationRepository
                .findByIdAndUserId(reservationId, user.getId())
                .orElseThrow(() -> new ReservationException(ReservationErrorCode.RSV_RESERVATION_NOT_FOUND));

        int count = (reservation.getSwitchScreenCount() == null ? 0 : reservation.getSwitchScreenCount()) + 1;
        reservation.setSwitchScreenCount(count);
        sessionReservationRepository.save(reservation);

        int threshold = proctorProperties.getForceSubmitSwitchCount();
        SwitchScreenResponse response = new SwitchScreenResponse();
        response.setSwitchScreenCount(count);
        response.setThreshold(threshold);

        if (count >= threshold) {
            // 达到上限：强制交卷（按已答内容计分，标记 FORCED），并以错误码告知前端
            examService.forceSubmitBySwitchScreen(reservation, answers);
            response.setForcedSubmitted(true);
            log.info("User [{}] reservation [ID: {}] force-submitted after {} screen switches (threshold {})",
                    username, reservationId, count, threshold);
            throw new ReservationException(ReservationErrorCode.RSV_FORCED_SUBMIT_SWITCH);
        }

        log.info("User [{}] reservation [ID: {}] switch-screen count now {} / {}",
                username, reservationId, count, threshold);
        return response;
    }

    private SessionResponse toSessionResponse(ExamSession session, Long userId) {
        long booked = sessionReservationRepository.countBySessionIdAndStatus(session.getId(), ReservationStatus.BOOKED);
        boolean reservedByMe = sessionReservationRepository
                .findBySessionIdAndUserIdAndStatus(session.getId(), userId, ReservationStatus.BOOKED)
                .isPresent();

        SessionResponse response = new SessionResponse();
        response.setId(session.getId());
        response.setExamId(session.getExam().getId());
        response.setExamTitle(session.getExam().getTitle());
        response.setExamDescription(session.getExam().getDescription());
        response.setStartTime(session.getStartTime());
        response.setDurationMinutes(session.getDurationMinutes());
        response.setCapacity(session.getCapacity());
        response.setRemainingSeats(Math.max(0, session.getCapacity() - booked));
        response.setReservedByMe(reservedByMe);
        return response;
    }

    private ReservationResponse toReservationResponse(SessionReservation reservation) {
        ExamSession session = reservation.getSession();
        ReservationResponse response = new ReservationResponse();
        response.setId(reservation.getId());
        response.setSessionId(session.getId());
        response.setExamId(session.getExam().getId());
        response.setExamTitle(session.getExam().getTitle());
        response.setStartTime(session.getStartTime());
        response.setDurationMinutes(session.getDurationMinutes());
        response.setReservedAt(reservation.getReservedAt());
        response.setStatus(reservation.getStatus());
        return response;
    }
}
