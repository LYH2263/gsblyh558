package com.exam.system.controller;

import com.exam.system.entity.PracticeRecord;
import com.exam.system.entity.Question;
import com.exam.system.entity.User;
import com.exam.system.entity.WrongQuestionRecord;
import com.exam.system.payload.request.PracticeSubmitRequest;
import com.exam.system.repository.PracticeRecordRepository;
import com.exam.system.repository.QuestionRepository;
import com.exam.system.repository.UserRepository;
import com.exam.system.repository.WrongQuestionRecordRepository;
import com.exam.system.payload.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/practice")
@Slf4j
public class PracticeController {

    @Autowired
    UserRepository userRepository;

    @Autowired
    QuestionRepository questionRepository;

    @Autowired
    PracticeRecordRepository practiceRecordRepository;

    @Autowired
    WrongQuestionRecordRepository wrongQuestionRecordRepository;

    @Autowired
    com.exam.system.service.QuestionService questionService;

    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<Boolean>> submitPractice(@Valid @RequestBody PracticeSubmitRequest request) {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username = userDetails.getUsername();
        
        log.info("User [{}] is submitting practice for question [ID: {}]", username, request.getQuestionId());

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("未找到该用户"));
        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new RuntimeException("未找到 ID 为 " + request.getQuestionId() + " 的题目"));

        boolean isCorrect = questionService.isAnswerCorrect(question, request.getUserAnswer());

        PracticeRecord record = new PracticeRecord();
        record.setUser(user);
        record.setQuestion(question);
        record.setUserAnswer(request.getUserAnswer());
        record.setIsCorrect(isCorrect);
        practiceRecordRepository.save(record);

        if (!isCorrect) {
            log.info("User [{}] answered question [ID: {}] incorrectly in practice. Updating wrong question book.", username, question.getId());
            WrongQuestionRecord wrongRecord = wrongQuestionRecordRepository
                    .findByUserIdAndQuestionIdAndIsRemovedFalse(user.getId(), question.getId())
                    .orElse(new WrongQuestionRecord());
            
            wrongRecord.setUser(user);
            wrongRecord.setQuestion(question);
            wrongRecord.setIsRemoved(false);
            wrongRecord.setLastWrongTime(LocalDateTime.now());
            wrongQuestionRecordRepository.save(wrongRecord);
        } else {
            log.info("User [{}] answered question [ID: {}] correctly in practice. Removing from wrong question book if exists.", username, question.getId());
            wrongQuestionRecordRepository
                    .findByUserIdAndQuestionIdAndIsRemovedFalse(user.getId(), question.getId())
                    .ifPresent(wr -> {
                        wr.setIsRemoved(true);
                        wr.setRemoveTime(LocalDateTime.now());
                        wrongQuestionRecordRepository.save(wr);
                        log.debug("Question [ID: {}] removed from user [{}] wrong question book.", question.getId(), username);
                    });
        }

        return ResponseEntity.ok(ApiResponse.success(isCorrect));
    }
}
