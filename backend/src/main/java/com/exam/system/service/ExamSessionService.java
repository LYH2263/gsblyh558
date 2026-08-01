package com.exam.system.service;

import com.exam.system.entity.Exam;
import com.exam.system.entity.ExamFinishType;
import com.exam.system.entity.ExamSession;
import com.exam.system.entity.ReservationStatus;
import com.exam.system.entity.SessionStatus;
import com.exam.system.exception.ReservationErrorCode;
import com.exam.system.exception.ReservationException;
import com.exam.system.exception.ResourceNotFoundException;
import com.exam.system.payload.request.ExamSessionRequest;
import com.exam.system.payload.response.AdminSessionResponse;
import com.exam.system.payload.response.SessionStatsResponse;
import com.exam.system.repository.ExamRepository;
import com.exam.system.repository.ExamResultRepository;
import com.exam.system.repository.ExamSessionRepository;
import com.exam.system.repository.SessionReservationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理端场次管理业务。跨表读写与事务集中在 Service 层。
 */
@Service
@Slf4j
public class ExamSessionService {

    @Autowired
    private ExamSessionRepository examSessionRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private SessionReservationRepository sessionReservationRepository;

    @Autowired
    private ExamResultRepository examResultRepository;

    @Transactional(readOnly = true)
    public List<AdminSessionResponse> listAll(SessionStatus status) {
        List<ExamSession> sessions = (status == null)
                ? examSessionRepository.findAll()
                : examSessionRepository.findByStatus(status);
        return sessions.stream()
                .map(this::toAdminResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AdminSessionResponse create(ExamSessionRequest request) {
        Exam exam = examRepository.findById(request.getExamId())
                .orElseThrow(() -> new ResourceNotFoundException("未找到 ID 为 " + request.getExamId() + " 的考试"));

        ExamSession session = new ExamSession();
        session.setExam(exam);
        session.setStartTime(request.getStartTime());
        session.setDurationMinutes(request.getDurationMinutes());
        session.setLateEntryMinutes(request.getLateEntryMinutes());
        session.setCapacity(request.getCapacity());
        session.setStatus(request.getStatus());

        ExamSession saved = examSessionRepository.save(session);
        log.info("Created exam session [ID: {}] for exam [ID: {}]", saved.getId(), exam.getId());
        return toAdminResponse(saved);
    }

    @Transactional
    public AdminSessionResponse update(Long id, ExamSessionRequest request) {
        ExamSession session = examSessionRepository.findById(id)
                .orElseThrow(() -> new ReservationException(ReservationErrorCode.RSV_SESSION_NOT_FOUND));

        Exam exam = examRepository.findById(request.getExamId())
                .orElseThrow(() -> new ResourceNotFoundException("未找到 ID 为 " + request.getExamId() + " 的考试"));

        session.setExam(exam);
        session.setStartTime(request.getStartTime());
        session.setDurationMinutes(request.getDurationMinutes());
        session.setLateEntryMinutes(request.getLateEntryMinutes());
        session.setCapacity(request.getCapacity());
        session.setStatus(request.getStatus());

        ExamSession saved = examSessionRepository.save(session);
        log.info("Updated exam session [ID: {}]", saved.getId());
        return toAdminResponse(saved);
    }

    /**
     * 下线（关闭）场次。若已有人预约且未携带确认标记，则返回当前预约人数供前端提示，不静默删除。
     */
    @Transactional
    public AdminSessionResponse close(Long id, boolean confirmed) {
        ExamSession session = examSessionRepository.findById(id)
                .orElseThrow(() -> new ReservationException(ReservationErrorCode.RSV_SESSION_NOT_FOUND));

        long booked = sessionReservationRepository.countBySessionIdAndStatus(id, ReservationStatus.BOOKED);
        if (booked > 0 && !confirmed) {
            // 未确认时不下线，交由前端根据 bookedCount 弹出人数提示
            AdminSessionResponse response = toAdminResponse(session);
            log.info("Close session [ID: {}] blocked pending confirm, {} reservations exist", id, booked);
            return response;
        }

        session.setStatus(SessionStatus.CLOSED);
        ExamSession saved = examSessionRepository.save(session);
        log.info("Closed exam session [ID: {}], affecting {} reservations", id, booked);
        return toAdminResponse(saved);
    }

    /**
     * 场次数据看板：按场次汇总预约/开考/正常/超时/强制人数与平均用时（分钟）。
     * 可按 SessionStatus 过滤。
     */
    @Transactional(readOnly = true)
    public List<SessionStatsResponse> listStats(SessionStatus status) {
        List<ExamSession> sessions = (status == null)
                ? examSessionRepository.findAll()
                : examSessionRepository.findByStatus(status);
        return sessions.stream()
                .map(this::toStatsResponse)
                .collect(Collectors.toList());
    }

    private SessionStatsResponse toStatsResponse(ExamSession session) {
        Long sid = session.getId();
        SessionStatsResponse stats = new SessionStatsResponse();
        stats.setSessionId(sid);
        stats.setExamTitle(session.getExam().getTitle());
        stats.setStartTime(session.getStartTime());
        stats.setStatus(session.getStatus());
        stats.setBookedCount(sessionReservationRepository.countBySessionIdAndStatus(sid, ReservationStatus.BOOKED));
        stats.setStartedCount(sessionReservationRepository
                .countBySessionIdAndStatusAndStartedAtIsNotNull(sid, ReservationStatus.BOOKED));
        stats.setNormalCount(examResultRepository.countBySessionIdAndFinishType(sid, ExamFinishType.NORMAL));
        stats.setTimeoutCount(examResultRepository.countBySessionIdAndFinishType(sid, ExamFinishType.TIMEOUT));
        stats.setForcedCount(examResultRepository.countBySessionIdAndFinishType(sid, ExamFinishType.FORCED));
        Double avg = examResultRepository.findAverageUsedMinutesBySessionId(sid);
        stats.setAverageUsedMinutes(avg == null ? 0 : (int) Math.round(avg));
        return stats;
    }

    private AdminSessionResponse toAdminResponse(ExamSession session) {
        long booked = sessionReservationRepository.countBySessionIdAndStatus(session.getId(), ReservationStatus.BOOKED);
        long started = sessionReservationRepository
                .countBySessionIdAndStatusAndStartedAtIsNotNull(session.getId(), ReservationStatus.BOOKED);
        AdminSessionResponse response = new AdminSessionResponse();
        response.setId(session.getId());
        response.setExamId(session.getExam().getId());
        response.setExamTitle(session.getExam().getTitle());
        response.setStartTime(session.getStartTime());
        response.setDurationMinutes(session.getDurationMinutes());
        response.setLateEntryMinutes(session.getLateEntryMinutes());
        response.setCapacity(session.getCapacity());
        response.setStatus(session.getStatus());
        response.setBookedCount(booked);
        response.setRemainingSeats(Math.max(0, session.getCapacity() - booked));
        response.setStartedCount(started);
        return response;
    }
}
