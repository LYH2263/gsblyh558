package com.exam.system.service;

import com.exam.system.dto.ReservationResponse;
import com.exam.system.dto.SessionAccessResponse;
import com.exam.system.dto.SwitchScreenResponse;
import com.exam.system.entity.ExamSession;
import com.exam.system.entity.ReservationStatus;
import com.exam.system.entity.SessionReservation;
import com.exam.system.entity.SessionStatus;
import com.exam.system.entity.User;
import com.exam.system.exception.BusinessException;
import com.exam.system.repository.ExamResultRepository;
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
import java.util.stream.Collectors;

@Slf4j
@Service
public class ReservationService {

    @Autowired
    private SessionReservationRepository reservationRepository;

    @Autowired
    private ExamSessionRepository sessionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExamResultRepository examResultRepository;

    /**
     * 预约场次。通过对场次行加悲观写锁串行化同一场次的预约，保证并发下不超卖。
     */
    @Transactional
    public ReservationResponse book(Long sessionId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(404, "RSV_USER_NOT_FOUND", "用户不存在"));

        ExamSession session = sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new BusinessException(404, "RSV_SESSION_NOT_FOUND", "场次不存在或已删除"));

        if (session.getStatus() != SessionStatus.OPEN) {
            throw new BusinessException(400, "RSV_SESSION_NOT_OPEN", "该场次未开放预约");
        }

        long minutesUntilStart = Duration.between(LocalDateTime.now(), session.getStartTime()).toMinutes();
        if (minutesUntilStart < ExamSessionService.BOOKING_LEAD_TIME_MINUTES) {
            throw new BusinessException(400, "RSV_TOO_CLOSE_TO_START",
                    "距场次开始不足 " + ExamSessionService.BOOKING_LEAD_TIME_MINUTES + " 分钟，无法预约");
        }

        if (reservationRepository.existsBySession_IdAndUser_IdAndStatus(sessionId, user.getId(), ReservationStatus.BOOKED)) {
            throw new BusinessException(400, "RSV_DUPLICATE_RESERVATION", "您已预约过该场次，不能重复预约");
        }

        long bookedCount = reservationRepository.countBySession_IdAndStatus(sessionId, ReservationStatus.BOOKED);
        if (bookedCount >= session.getCapacity()) {
            throw new BusinessException(400, "RSV_SESSION_FULL", "该场次名额已满");
        }

        SessionReservation reservation = new SessionReservation();
        reservation.setSession(session);
        reservation.setUser(user);
        reservation.setReservedAt(LocalDateTime.now());
        reservation.setStatus(ReservationStatus.BOOKED);

        SessionReservation saved = reservationRepository.save(reservation);
        log.info("User [{}] booked session [ID: {}], reservation [ID: {}]", username, sessionId, saved.getId());
        return toResponse(saved);
    }

    /**
     * 取消预约：仅本人可取消，且仅 BOOKED 状态可取消；取消后名额即时释放。
     */
    @Transactional
    public void cancel(Long reservationId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(404, "RSV_USER_NOT_FOUND", "用户不存在"));

        SessionReservation reservation = reservationRepository.findByIdAndUser_Id(reservationId, user.getId())
                .orElseThrow(() -> new BusinessException(404, "RSV_RESERVATION_NOT_FOUND", "预约记录不存在"));

        if (reservation.getStatus() != ReservationStatus.BOOKED) {
            throw new BusinessException(400, "RSV_RESERVATION_NOT_ACTIVE", "该预约已取消，无法重复操作");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);
        log.info("User [{}] cancelled reservation [ID: {}] for session [ID: {}]",
                username, reservationId, reservation.getSession().getId());
    }

    /**
     * 开考准入校验：仅当用户已预约该场次、预约有效且当前时间落在作答窗口内时放行。
     * 作答窗口为 [startTime, startTime + durationMinutes]，
     * 且进入时间不得晚于 startTime + lateEntryMinutes（迟到宽限量）。
     * 首次通过校验时记录开考时间（startedAt）。
     */
    @Transactional
    public SessionAccessResponse checkAccess(Long sessionId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(404, "RSV_USER_NOT_FOUND", "用户不存在"));

        ExamSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(404, "RSV_SESSION_NOT_FOUND", "场次不存在或已删除"));

        SessionReservation reservation = reservationRepository.findBySession_IdAndUser_Id(sessionId, user.getId())
                .orElseThrow(() -> new BusinessException(403, "RSV_NOT_RESERVED", "您尚未预约该场次，请先完成预约"));

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new BusinessException(400, "RSV_RESERVATION_CANCELLED", "您的预约已取消，无法进入考试");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = session.getStartTime().plusMinutes(session.getDurationMinutes());
        LocalDateTime latestEntryTime = session.getStartTime().plusMinutes(session.getLateEntryMinutes());

        if (now.isBefore(session.getStartTime())) {
            throw new BusinessException(400, "RSV_SESSION_NOT_STARTED", "场次尚未开始，请稍后再来");
        }
        if (now.isAfter(endTime) || now.isAfter(latestEntryTime)) {
            throw new BusinessException(400, "RSV_SESSION_ENDED", "场次已结束，无法进入考试");
        }

        if (reservation.getStartedAt() == null) {
            reservation.setStartedAt(now);
            reservationRepository.save(reservation);
        }

        long remainingMinutes = Math.max(0, Duration.between(now, endTime).toMinutes());
        log.info("User [{}] entered session [ID: {}], remaining {} minutes", username, sessionId, remainingMinutes);

        return SessionAccessResponse.builder()
                .sessionId(session.getId())
                .reservationId(reservation.getId())
                .examId(session.getExam().getId())
                .examTitle(session.getExam().getTitle())
                .startTime(session.getStartTime())
                .durationMinutes(session.getDurationMinutes())
                .lateEntryMinutes(session.getLateEntryMinutes())
                .maxSwitchCount(session.getMaxSwitchCount())
                .endTime(endTime)
                .remainingMinutes(remainingMinutes)
                .build();
    }

    /**
     * 切屏上报：与预约记录关联落库累计次数；达到场次上限时返回 forceSubmit，
     * 由前端立即强制交卷；已交卷的预约拒绝继续作答。
     */
    @Transactional
    public SwitchScreenResponse recordSwitchScreen(Long reservationId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(404, "RSV_USER_NOT_FOUND", "用户不存在"));

        SessionReservation reservation = reservationRepository.findByIdAndUser_Id(reservationId, user.getId())
                .orElseThrow(() -> new BusinessException(404, "RSV_RESERVATION_NOT_FOUND", "预约记录不存在"));

        if (reservation.getStatus() != ReservationStatus.BOOKED) {
            throw new BusinessException(400, "RSV_RESERVATION_NOT_ACTIVE", "该预约已取消，无法继续作答");
        }

        ExamSession session = reservation.getSession();
        if (examResultRepository.existsByUser_IdAndSession_Id(user.getId(), session.getId())) {
            throw new BusinessException(400, "RSV_ALREADY_SUBMITTED", "本场次已交卷，无法继续作答");
        }

        reservation.setSwitchCount(reservation.getSwitchCount() + 1);
        reservationRepository.save(reservation);

        int limit = session.getMaxSwitchCount();
        boolean forceSubmit = reservation.getSwitchCount() >= limit;
        log.info("User [{}] switched screen in session [ID: {}], count {}/{} (forceSubmit: {})",
                username, session.getId(), reservation.getSwitchCount(), limit, forceSubmit);

        return SwitchScreenResponse.builder()
                .switchCount(reservation.getSwitchCount())
                .maxSwitchCount(limit)
                .forceSubmit(forceSubmit)
                .build();
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> myReservations(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(404, "RSV_USER_NOT_FOUND", "用户不存在"));
        return reservationRepository.findByUser_IdOrderByReservedAtDesc(user.getId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private ReservationResponse toResponse(SessionReservation reservation) {
        ExamSession session = reservation.getSession();
        return ReservationResponse.builder()
                .id(reservation.getId())
                .sessionId(session.getId())
                .examId(session.getExam().getId())
                .examTitle(session.getExam().getTitle())
                .startTime(session.getStartTime())
                .durationMinutes(session.getDurationMinutes())
                .reservedAt(reservation.getReservedAt())
                .status(reservation.getStatus())
                .build();
    }
}
