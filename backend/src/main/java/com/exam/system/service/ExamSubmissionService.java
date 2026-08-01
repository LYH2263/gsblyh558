package com.exam.system.service;

import com.exam.system.entity.*;
import com.exam.system.repository.ExamRecordRepository;
import com.exam.system.repository.ExamResultRepository;
import com.exam.system.repository.WrongQuestionRecordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@Slf4j
public class ExamSubmissionService {
    @Autowired
    private ExamResultRepository examResultRepository;

    @Autowired
    private ExamRecordRepository examRecordRepository;

    @Autowired
    private WrongQuestionRecordRepository wrongQuestionRecordRepository;

    @Autowired
    private QuestionService questionService;

    @Transactional
    public ExamResult submitDirect(User user, Exam exam, Map<Long, String> answers) {
        int score = calculateScoreAndUpdateWrongBook(user, exam, answers);
        ExamResult result = new ExamResult();
        result.setUser(user);
        result.setExam(exam);
        result.setScore(score);
        result.setTimedOut(false);
        return examResultRepository.save(result);
    }

    @Transactional
    public ExamResult submitForReservation(User user, Exam exam, ExamSession session,
                                           SessionReservation reservation,
                                           Map<Long, String> answers,
                                           boolean timedOut,
                                           ForcedSubmitReason forcedSubmitReason) {
        int score = calculateScoreAndUpdateWrongBook(user, exam, answers);
        ExamResult result = new ExamResult();
        result.setUser(user);
        result.setExam(exam);
        result.setSession(session);
        result.setReservation(reservation);
        result.setScore(score);
        result.setTimedOut(timedOut);
        result.setForcedSubmitReason(forcedSubmitReason);
        ExamResult saved = examResultRepository.save(result);

        examRecordRepository.findByReservation(reservation).ifPresent(record -> {
            record.setSubmitTime(LocalDateTime.now());
            record.setScore(score);
            examRecordRepository.save(record);
        });

        return saved;
    }

    private int calculateScoreAndUpdateWrongBook(User user, Exam exam, Map<Long, String> answers) {
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
                            log.info("Question [ID: {}] removed from user [{}] wrong question book.", question.getId(), user.getUsername());
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
