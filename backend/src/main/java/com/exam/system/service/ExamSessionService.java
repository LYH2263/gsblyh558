package com.exam.system.service;

import com.exam.system.dto.ExamSessionRequest;
import com.exam.system.dto.ExamSessionResponse;
import com.exam.system.dto.SessionStatsResponse;
import com.exam.system.entity.Exam;
import com.exam.system.entity.ExamSession;
import com.exam.system.entity.FinishType;
import com.exam.system.entity.ReservationStatus;
import com.exam.system.entity.SessionStatus;
import com.exam.system.exception.BusinessException;
import com.exam.system.repository.ExamRepository;
import com.exam.system.repository.ExamResultRepository;
import com.exam.system.repository.ExamSessionRepository;
import com.exam.system.repository.SessionReservationRepository;
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
public class ExamSessionService {

    /** 预约时间门槛：只能预约距开始时间 >= 该分钟数的场次 */
    public static final long BOOKING_LEAD_TIME_MINUTES = 30;

    @Autowired
    private ExamSessionRepository sessionRepository;

    @Autowired
    private SessionReservationRepository reservationRepository;

    @Autowired
    private ExamResultRepository examResultRepository;

    @Autowired
    private ExamRepository examRepository;

    @Transactional(readOnly = true)
    public List<ExamSessionResponse> listSessions(Long examId, SessionStatus status) {
        List<ExamSession> sessions;
        if (examId != null && status != null) {
            sessions = sessionRepository.findByExamIdAndStatusOrderByStartTimeAsc(examId, status);
        } else if (examId != null) {
            sessions = sessionRepository.findByExamIdOrderByStartTimeAsc(examId);
        } else if (status != null) {
            sessions = sessionRepository.findByStatusOrderByStartTimeAsc(status);
        } else {
            sessions = sessionRepository.findAll();
        }
        return sessions.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /** 用户端可预约场次列表：仅返回 OPEN 状态的场次 */
    @Transactional(readOnly = true)
    public List<ExamSessionResponse> listBookableSessions(Long examId) {
        List<ExamSession> sessions = (examId != null)
                ? sessionRepository.findByExamIdAndStatusOrderByStartTimeAsc(examId, SessionStatus.OPEN)
                : sessionRepository.findByStatusOrderByStartTimeAsc(SessionStatus.OPEN);
        return sessions.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public ExamSessionResponse createSession(ExamSessionRequest request) {
        Exam exam = examRepository.findById(request.getExamId())
                .orElseThrow(() -> new BusinessException(404, "RSV_EXAM_NOT_FOUND", "考试不存在"));

        ExamSession session = new ExamSession();
        session.setExam(exam);
        session.setStartTime(request.getStartTime());
        session.setDurationMinutes(request.getDurationMinutes());
        session.setCapacity(request.getCapacity());
        session.setLateEntryMinutes(request.getLateEntryMinutes() != null ? request.getLateEntryMinutes() : 10);
        session.setMaxSwitchCount(request.getMaxSwitchCount() != null ? request.getMaxSwitchCount() : 3);
        session.setStatus(SessionStatus.OPEN);

        ExamSession saved = sessionRepository.save(session);
        log.info("Exam session [ID: {}] created for exam [ID: {}], startTime: {}", saved.getId(), exam.getId(), saved.getStartTime());
        return toResponse(saved);
    }

    @Transactional
    public ExamSessionResponse updateSession(Long id, ExamSessionRequest request) {
        ExamSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "RSV_SESSION_NOT_FOUND", "场次不存在"));

        long bookedCount = reservationRepository.countBySession_IdAndStatus(id, ReservationStatus.BOOKED);
        if (request.getCapacity() < bookedCount) {
            throw new BusinessException(400, "RSV_CAPACITY_BELOW_BOOKED",
                    "该场次已有 " + bookedCount + " 人预约，最大预约人数不能低于该值");
        }

        session.setStartTime(request.getStartTime());
        session.setDurationMinutes(request.getDurationMinutes());
        session.setCapacity(request.getCapacity());
        if (request.getLateEntryMinutes() != null) {
            session.setLateEntryMinutes(request.getLateEntryMinutes());
        }
        if (request.getMaxSwitchCount() != null) {
            session.setMaxSwitchCount(request.getMaxSwitchCount());
        }

        ExamSession saved = sessionRepository.save(session);
        log.info("Exam session [ID: {}] updated", id);
        return toResponse(saved);
    }

    /**
     * 下线场次：状态置为 CLOSED，返回当前有效预约人数（用于给管理员人数提示）。
     */
    @Transactional
    public long closeSession(Long id) {
        ExamSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "RSV_SESSION_NOT_FOUND", "场次不存在"));

        long bookedCount = reservationRepository.countBySession_IdAndStatus(id, ReservationStatus.BOOKED);
        session.setStatus(SessionStatus.CLOSED);
        sessionRepository.save(session);
        log.info("Exam session [ID: {}] closed with {} active reservations", id, bookedCount);
        return bookedCount;
    }

    /** 场次数据看板：按场次统计预约/开考/按时完成/超时交卷/切屏强制交卷人数与平均用时 */
    @Transactional(readOnly = true)
    public List<SessionStatsResponse> sessionStats(Long examId, SessionStatus status) {
        List<ExamSession> sessions;
        if (examId != null && status != null) {
            sessions = sessionRepository.findByExamIdAndStatusOrderByStartTimeAsc(examId, status);
        } else if (examId != null) {
            sessions = sessionRepository.findByExamIdOrderByStartTimeAsc(examId);
        } else if (status != null) {
            sessions = sessionRepository.findByStatusOrderByStartTimeAsc(status);
        } else {
            sessions = sessionRepository.findAll();
        }
        return sessions.stream().map(session -> SessionStatsResponse.builder()
                .sessionId(session.getId())
                .examId(session.getExam().getId())
                .examTitle(session.getExam().getTitle())
                .startTime(session.getStartTime())
                .durationMinutes(session.getDurationMinutes())
                .status(session.getStatus())
                .bookedCount(reservationRepository.countBySession_IdAndStatus(session.getId(), ReservationStatus.BOOKED))
                .startedCount(reservationRepository.countBySession_IdAndStartedAtIsNotNull(session.getId()))
                .finishedCount(examResultRepository.countBySession_IdAndFinishType(session.getId(), FinishType.NORMAL))
                .timeoutCount(examResultRepository.countBySession_IdAndFinishType(session.getId(), FinishType.TIMEOUT))
                .forcedCount(examResultRepository.countBySession_IdAndFinishType(session.getId(), FinishType.FORCED))
                .avgUsedMinutes(Math.round(examResultRepository.avgUsedMinutesBySessionId(session.getId())))
                .build()
        ).collect(Collectors.toList());
    }

    private ExamSessionResponse toResponse(ExamSession session) {
        long bookedCount = reservationRepository.countBySession_IdAndStatus(session.getId(), ReservationStatus.BOOKED);
        long startedCount = reservationRepository.countBySession_IdAndStartedAtIsNotNull(session.getId());
        long remaining = Math.max(0, session.getCapacity() - bookedCount);
        long minutesUntilStart = Duration.between(LocalDateTime.now(), session.getStartTime()).toMinutes();
        boolean bookable = session.getStatus() == SessionStatus.OPEN
                && remaining > 0
                && minutesUntilStart >= BOOKING_LEAD_TIME_MINUTES;
        return ExamSessionResponse.builder()
                .id(session.getId())
                .examId(session.getExam().getId())
                .examTitle(session.getExam().getTitle())
                .startTime(session.getStartTime())
                .durationMinutes(session.getDurationMinutes())
                .capacity(session.getCapacity())
                .lateEntryMinutes(session.getLateEntryMinutes())
                .maxSwitchCount(session.getMaxSwitchCount())
                .status(session.getStatus())
                .bookedCount(bookedCount)
                .startedCount(startedCount)
                .remainingQuota(remaining)
                .minutesUntilStart(minutesUntilStart)
                .bookable(bookable)
                .build();
    }
}
