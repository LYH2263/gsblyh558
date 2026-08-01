package com.exam.system.service;

import com.exam.system.entity.Exam;
import com.exam.system.entity.ExamQuestion;
import com.exam.system.entity.Question;
import com.exam.system.entity.User;
import com.exam.system.entity.WrongQuestionRecord;
import com.exam.system.repository.WrongQuestionRecordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
public class ExamGradingService {

    @Autowired
    private QuestionService questionService;

    @Autowired
    private WrongQuestionRecordRepository wrongQuestionRecordRepository;

    @Transactional
    public int gradeExam(Exam exam, User user, Map<Long, String> answers) {
        int totalScore = 0;
        if (exam.getQuestions() == null) {
            return totalScore;
        }
        for (ExamQuestion examQuestion : exam.getQuestions()) {
            Question question = examQuestion.getQuestion();
            String userAnswer = answers == null ? null : answers.get(question.getId());

            boolean isCorrect = questionService.isAnswerCorrect(question, userAnswer);

            if (isCorrect) {
                totalScore += examQuestion.getScore() != null ? examQuestion.getScore() : 0;
                wrongQuestionRecordRepository
                        .findByUserIdAndQuestionIdAndIsRemovedFalse(user.getId(), question.getId())
                        .ifPresent(wr -> {
                            wr.setIsRemoved(true);
                            wr.setRemoveTime(LocalDateTime.now());
                            wrongQuestionRecordRepository.save(wr);
                            log.info("Question [ID: {}] removed from user [{}] wrong question book.",
                                    question.getId(), user.getUsername());
                        });
            } else {
                log.info("User [{}] answered question [ID: {}] incorrectly. Adding/Updating wrong question book.",
                        user.getUsername(), question.getId());
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
