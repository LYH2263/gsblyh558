package com.exam.system.service;

import com.exam.system.dto.ExamSubmission;
import com.exam.system.entity.Exam;
import com.exam.system.entity.ExamFinishType;
import com.exam.system.entity.ExamQuestion;
import com.exam.system.entity.ExamResult;
import com.exam.system.entity.ExamSession;
import com.exam.system.entity.Question;
import com.exam.system.entity.SessionReservation;
import com.exam.system.entity.User;
import com.exam.system.entity.WrongQuestionRecord;
import com.exam.system.exception.ReservationErrorCode;
import com.exam.system.exception.ReservationException;
import com.exam.system.exception.ResourceNotFoundException;
import com.exam.system.repository.ExamRepository;
import com.exam.system.repository.ExamResultRepository;
import com.exam.system.repository.ExamSessionRepository;
import com.exam.system.repository.SessionReservationRepository;
import com.exam.system.repository.UserRepository;
import com.exam.system.repository.WrongQuestionRecordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ExamService {
    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private ExamResultRepository examResultRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WrongQuestionRecordRepository wrongQuestionRecordRepository;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private ExamSessionRepository examSessionRepository;

    @Autowired
    private SessionReservationRepository sessionReservationRepository;

    public List<Exam> getAllExams() {
        return examRepository.findAll();
    }

    public Exam createExam(Exam exam) {
        return examRepository.save(exam);
    }

    public Exam getExamById(Long id) {
        return examRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Exam not found"));
    }

    /**
     * 交卷。跨表业务与事务集中在 Service 层（公约第 2 条）。
     * 服务端独立判定超时：若本次作答关联场次，且提交时刻已超出作答窗口（startTime + durationMinutes），
     * 则本次作答按超时（TIMEOUT）落库，并以 RSV_SUBMIT_TIMEOUT 错误码返回给前端。
     * 采用 noRollbackFor 确保超时成绩仍然落库不回滚。
     */
    @Transactional(noRollbackFor = ReservationException.class)
    public ExamResult submitExam(Long examId, String username, ExamSubmission submission) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("未找到该用户"));

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("未找到 ID 为 " + examId + " 的考试"));

        // 定位本次作答关联的场次与预约（若走场次预约途径）
        ExamSession session = null;
        SessionReservation reservation = null;
        boolean timeout = false;

        if (submission.getReservationId() != null) {
            reservation = sessionReservationRepository
                    .findByIdAndUserId(submission.getReservationId(), user.getId())
                    .orElseThrow(() -> new ReservationException(ReservationErrorCode.RSV_RESERVATION_NOT_FOUND));
            session = reservation.getSession();

            // 服务端独立超时判定：以服务端时间为准，忽略前端 autoSubmitted 标记
            LocalDateTime deadline = session.getStartTime().plusMinutes(session.getDurationMinutes());
            if (LocalDateTime.now().isAfter(deadline)) {
                timeout = true;
            }
        }

        ExamFinishType finishType = timeout ? ExamFinishType.TIMEOUT : ExamFinishType.NORMAL;
        ExamResult savedResult = gradeAndPersist(exam, user, session, reservation,
                submission.getAnswers(), finishType, username);

        if (timeout) {
            // 超时：成绩已落库（noRollbackFor），以错误码告知前端
            throw new ReservationException(ReservationErrorCode.RSV_SUBMIT_TIMEOUT);
        }
        return savedResult;
    }

    /**
     * 因切屏达到上限被强制交卷。按已作答内容计分并以 FORCED 标记原因落库。
     * 供 ReservationService 在切屏上报达到阈值时调用（同一事务内）。
     */
    @Transactional
    public ExamResult forceSubmitBySwitchScreen(SessionReservation reservation, Map<Long, String> answers) {
        User user = reservation.getUser();
        Exam exam = reservation.getSession().getExam();
        // exam 可能是懒代理，确保题目可用
        exam = examRepository.findById(exam.getId())
                .orElseThrow(() -> new ResourceNotFoundException("未找到对应考试"));
        return gradeAndPersist(exam, user, reservation.getSession(), reservation,
                answers, ExamFinishType.FORCED, user.getUsername());
    }

    /**
     * 评分 + 更新错题本 + 组装并保存作答结果，计算用时（分钟，第 3 条）。
     */
    private ExamResult gradeAndPersist(Exam exam, User user, ExamSession session, SessionReservation reservation,
                                       Map<Long, String> answers, ExamFinishType finishType, String username) {
        int totalScore = gradeAndUpdateWrongBook(exam, user, answers);

        ExamResult result = new ExamResult();
        result.setUser(user);
        result.setExam(exam);
        result.setSession(session);
        result.setReservation(reservation);
        result.setScore(totalScore);
        result.setFinishType(finishType);
        if (session != null) {
            long used = Duration.between(session.getStartTime(), LocalDateTime.now()).toMinutes();
            result.setUsedMinutes((int) Math.max(0, used));
        }

        ExamResult savedResult = examResultRepository.save(result);
        log.info("User [{}] finished exam [ID: {}] with score: {}, finishType: {}, usedMinutes: {}",
                username, exam.getId(), totalScore, finishType, result.getUsedMinutes());
        return savedResult;
    }

    private int gradeAndUpdateWrongBook(Exam exam, User user, Map<Long, String> answers) {
        int totalScore = 0;
        for (ExamQuestion examQuestion : exam.getQuestions()) {
            Question question = examQuestion.getQuestion();
            String userAnswer = answers == null ? null : answers.get(question.getId());

            boolean isCorrect = questionService.isAnswerCorrect(question, userAnswer);

            if (isCorrect) {
                totalScore += examQuestion.getScore();
                wrongQuestionRecordRepository
                        .findByUserIdAndQuestionIdAndIsRemovedFalse(user.getId(), question.getId())
                        .ifPresent(wr -> {
                            wr.setIsRemoved(true);
                            wr.setRemoveTime(LocalDateTime.now());
                            wrongQuestionRecordRepository.save(wr);
                        });
            } else {
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
        return totalScore;
    }
}
