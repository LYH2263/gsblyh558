package com.exam.system.service;

import com.exam.system.dto.SessionDashboardRow;
import com.exam.system.entity.ExamResult;
import com.exam.system.entity.ExamSession;
import com.exam.system.entity.ReservationStatus;
import com.exam.system.entity.SessionReservation;
import com.exam.system.entity.SessionStatus;
import com.exam.system.repository.ExamResultRepository;
import com.exam.system.repository.ExamSessionRepository;
import com.exam.system.repository.SessionReservationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SessionDashboardService {

    @Autowired
    private ExamSessionRepository sessionRepository;

    @Autowired
    private SessionReservationRepository reservationRepository;

    @Autowired
    private ExamResultRepository examResultRepository;

    @Transactional(readOnly = true)
    public List<SessionDashboardRow> getDashboard(SessionStatus status) {
        List<ExamSession> sessions;
        if (status != null) {
            sessions = sessionRepository.findByStatusOrderByStartTimeAsc(status);
        } else {
            sessions = sessionRepository.findAllByOrderByStartTimeAsc();
        }

        return sessions.stream().map(this::toRow).collect(Collectors.toList());
    }

    private SessionDashboardRow toRow(ExamSession session) {
        SessionDashboardRow row = new SessionDashboardRow();
        row.setSessionId(session.getId());
        row.setExamTitle(session.getExam() != null ? session.getExam().getTitle() : null);
        row.setSessionStartTime(session.getStartTime());
        row.setDurationMinutes(session.getDurationMinutes());
        row.setStatus(session.getStatus() != null ? session.getStatus().name() : null);
        row.setCapacity(session.getCapacity());

        List<SessionReservation> booked = reservationRepository
                .findBySessionIdAndStatus(session.getId(), ReservationStatus.BOOKED);

        int reservedCount = booked.size();
        int startedCount = (int) booked.stream().filter(r -> r.getStartedAt() != null).count();
        long screenSwitchForcedCount = booked.stream()
                .filter(r -> Boolean.TRUE.equals(r.getForceSubmitted()))
                .count();

        List<ExamResult> results = examResultRepository.findBySessionId(session.getId());

        long timeoutCount = results.stream()
                .filter(r -> Boolean.TRUE.equals(r.getTimeout()))
                .count();
        long forcedByScreen = results.stream()
                .filter(r -> Boolean.TRUE.equals(r.getScreenSwitchForced()))
                .count();

        // On-time completed: has a result that is neither a timeout nor a screen-switch forced submission.
        long completedOnTime = results.stream()
                .filter(r -> !Boolean.TRUE.equals(r.getTimeout()))
                .filter(r -> !Boolean.TRUE.equals(r.getScreenSwitchForced()))
                .count();

        row.setReservedCount(reservedCount);
        row.setStartedCount(startedCount);
        row.setCompletedOnTimeCount((int) completedOnTime);
        row.setTimeoutSubmissionCount((int) timeoutCount);
        row.setScreenSwitchForcedCount((int) Math.max(screenSwitchForcedCount, forcedByScreen));
        row.setAverageDurationMinutes(computeAverageDurationMinutes(booked, results, session));
        return row;
    }

    private Integer computeAverageDurationMinutes(List<SessionReservation> booked,
                                                  List<ExamResult> results,
                                                  ExamSession session) {
        // Build reservationId -> startedAt map
        Map<Long, LocalDateTime> startedAtMap = new HashMap<>();
        for (SessionReservation r : booked) {
            if (r.getStartedAt() != null) {
                startedAtMap.put(r.getId(), r.getStartedAt());
            }
        }

        long totalMinutes = 0;
        int count = 0;
        for (ExamResult result : results) {
            if (result.getReservation() == null || result.getSubmitTime() == null) {
                continue;
            }
            LocalDateTime startedAt = startedAtMap.get(result.getReservation().getId());
            if (startedAt == null) {
                continue;
            }
            long minutes = Duration.between(startedAt, result.getSubmitTime()).toMinutes();
            if (minutes < 0) {
                continue;
            }
            // Cap to session duration to avoid outliers skewing the average.
            if (session.getDurationMinutes() != null && minutes > session.getDurationMinutes()) {
                minutes = session.getDurationMinutes();
            }
            totalMinutes += minutes;
            count++;
        }
        if (count == 0) {
            return 0;
        }
        return (int) Math.round((double) totalMinutes / count);
    }
}
