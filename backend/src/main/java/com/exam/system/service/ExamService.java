package com.exam.system.service;

import com.exam.system.dto.ExamSubmission;
import com.exam.system.entity.*;
import com.exam.system.exception.BusinessException;
import com.exam.system.exception.ResourceNotFoundException;
import com.exam.system.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ExamService {
    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private ExamResultRepository examResultRepository;

    @Autowired
    private ExamSessionRepository examSessionRepository;

    @Autowired
    private SessionReservationRepository sessionReservationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WrongQuestionRecordRepository wrongQuestionRecordRepository;

    @Autowired
    private QuestionService questionService;

    public List<Exam> getAllExams() {
        return examRepository.findAll();
    }

    public Exam getExamById(Long id) {
        return examRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found with id " + id));
    }

    @Transactional
    public Exam createExam(Exam exam) {
        validateExamScore(exam);
        if (exam.getQuestions() != null) {
            for (ExamQuestion eq : exam.getQuestions()) {
                eq.setExam(exam);
            }
        }
        return examRepository.save(exam);
    }

    @Transactional
    public Exam updateExam(Long id, Exam examRequest) {
        validateExamScore(examRequest);
        Exam exam = getExamById(id);
        exam.setTitle(examRequest.getTitle());
        exam.setDescription(examRequest.getDescription());
        exam.setTotalScore(examRequest.getTotalScore());
        exam.setDuration(examRequest.getDuration());

        if (exam.getQuestions() != null) {
            exam.getQuestions().clear();
        }
        if (examRequest.getQuestions() != null) {
            exam.getQuestions().addAll(examRequest.getQuestions());
            for (ExamQuestion eq : exam.getQuestions()) {
                eq.setExam(exam);
            }
        }
        return examRepository.save(exam);
    }

    @Transactional
    public void deleteExam(Long id) {
        examRepository.deleteById(id);
    }

    private void validateExamScore(Exam exam) {
        if (exam.getQuestions() == null || exam.getQuestions().isEmpty()) {
            log.warn("Attempt to save exam with no questions");
            throw new RuntimeException("考试至少需要包含一道题目");
        }
        int calculatedTotalScore = exam.getQuestions().stream()
                .mapToInt(eq -> eq.getScore() != null ? eq.getScore() : 0)
                .sum();
        if (calculatedTotalScore != exam.getTotalScore()) {
            log.warn("Exam score mismatch: calculated={}, declared={}", calculatedTotalScore, exam.getTotalScore());
            throw new RuntimeException("已选题目总分 (" + calculatedTotalScore + ") 与设定的考试总分 (" + exam.getTotalScore() + ") 不一致");
        }
    }

    /**
     * 交卷。服务端独立判定交卷方式：
     * - 切屏次数达到场次上限 -> FORCED（强制交卷，按已作答内容计分并标记原因）；
     * - 超出作答窗口（startTime + durationMinutes）-> TIMEOUT（成绩落库后以 RSV_SUBMIT_TIMEOUT 拒绝）；
     * - 其余 -> NORMAL。
     * noRollbackFor 保证超时落库的成绩不因业务异常回滚。
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public ExamResult submitExam(Long examId, ExamSubmission submission, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("未找到该用户"));

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("未找到 ID 为 " + examId + " 的考试"));

        ExamSession session = null;
        FinishType finishType = FinishType.NORMAL;
        Integer usedMinutes = null;

        if (submission.getSessionId() != null) {
            session = examSessionRepository.findById(submission.getSessionId())
                    .orElseThrow(() -> new BusinessException(404, "RSV_SESSION_NOT_FOUND", "场次不存在或已删除"));

            if (!session.getExam().getId().equals(exam.getId())) {
                throw new BusinessException(400, "RSV_SESSION_EXAM_MISMATCH", "场次与考试不匹配");
            }

            SessionReservation reservation;
            if (submission.getReservationId() != null) {
                reservation = sessionReservationRepository.findByIdAndUser_Id(submission.getReservationId(), user.getId())
                        .orElseThrow(() -> new BusinessException(404, "RSV_RESERVATION_NOT_FOUND", "预约记录不存在"));
                if (!reservation.getSession().getId().equals(session.getId())) {
                    throw new BusinessException(400, "RSV_RESERVATION_NOT_FOUND", "预约记录与场次不匹配");
                }
            } else {
                reservation = sessionReservationRepository.findBySession_IdAndUser_Id(session.getId(), user.getId())
                        .orElseThrow(() -> new BusinessException(403, "RSV_NOT_RESERVED", "您尚未预约该场次"));
            }

            if (reservation.getStatus() == ReservationStatus.CANCELLED) {
                throw new BusinessException(400, "RSV_RESERVATION_CANCELLED", "您的预约已取消，无法提交试卷");
            }

            if (examResultRepository.existsByUser_IdAndSession_Id(user.getId(), session.getId())) {
                throw new BusinessException(400, "RSV_ALREADY_SUBMITTED", "本场次已提交过试卷，不能重复提交");
            }

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime endTime = session.getStartTime().plusMinutes(session.getDurationMinutes());
            LocalDateTime answerStart = reservation.getStartedAt() != null ? reservation.getStartedAt() : session.getStartTime();
            usedMinutes = (int) Math.max(0, Duration.between(answerStart, now).toMinutes());

            if (reservation.getSwitchCount() >= session.getMaxSwitchCount()) {
                finishType = FinishType.FORCED;
            } else if (now.isAfter(endTime)) {
                finishType = FinishType.TIMEOUT;
            }
        }

        int totalScore = 0;
        Map<Long, String> answers = submission.getAnswers();

        for (ExamQuestion examQuestion : exam.getQuestions()) {
            Question question = examQuestion.getQuestion();
            String userAnswer = answers.get(question.getId());

            boolean isCorrect = questionService.isAnswerCorrect(question, userAnswer);

            if (isCorrect) {
                totalScore += examQuestion.getScore();
                // Remove from wrong question book if it exists
                wrongQuestionRecordRepository
                        .findByUserIdAndQuestionIdAndIsRemovedFalse(user.getId(), question.getId())
                        .ifPresent(wr -> {
                            wr.setIsRemoved(true);
                            wr.setRemoveTime(LocalDateTime.now());
                            wrongQuestionRecordRepository.save(wr);
                            log.info("Question [ID: {}] removed from user [{}] wrong question book because it was answered correctly in exam [ID: {}].", question.getId(), username, examId);
                        });
            } else {
                // Add to wrong question book
                log.info("User [{}] answered question [ID: {}] incorrectly in exam [ID: {}]. Adding/Updating wrong question book.", username, question.getId(), examId);
                WrongQuestionRecord wrongRecord = wrongQuestionRecordRepository
                        .findByUserIdAndQuestionIdAndIsRemovedFalse(user.getId(), question.getId())
                        .orElse(new WrongQuestionRecord());

                wrongRecord.setUser(user);
                wrongRecord.setQuestion(question);
                wrongRecord.setIsRemoved(false);
                wrongRecord.setLastWrongTime(LocalDateTime.now());
                wrongQuestionRecordRepository.save(wrongRecord);
            }
        }

        ExamResult result = new ExamResult();
        result.setUser(user);
        result.setExam(exam);
        result.setSession(session);
        result.setFinishType(finishType);
        result.setUsedMinutes(usedMinutes);
        result.setScore(totalScore);

        ExamResult savedResult = examResultRepository.save(result);
        log.info("User [{}] finished exam [ID: {}] with score: {} (finishType: {})", username, examId, totalScore, finishType);

        if (finishType == FinishType.TIMEOUT) {
            throw new BusinessException(400, "RSV_SUBMIT_TIMEOUT", "已超出作答窗口，本次作答已按超时交卷落库");
        }
        return savedResult;
    }
}
