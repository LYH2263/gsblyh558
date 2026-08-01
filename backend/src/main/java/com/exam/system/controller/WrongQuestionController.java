package com.exam.system.controller;

import com.exam.system.payload.response.ApiResponse;
import com.exam.system.entity.WrongQuestionRecord;
import com.exam.system.repository.UserRepository;
import com.exam.system.repository.WrongQuestionRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/wrong-questions")
public class WrongQuestionController {

    @Autowired
    WrongQuestionRecordRepository wrongQuestionRecordRepository;

    @Autowired
    UserRepository userRepository;

    @GetMapping
    public ApiResponse<List<WrongQuestionRecord>> getWrongQuestions() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userRepository.findByUsername(userDetails.getUsername()).orElseThrow().getId();
        
        return ApiResponse.success(wrongQuestionRecordRepository.findAll().stream()
                .filter(w -> w.getUser().getId().equals(userId) && !w.getIsRemoved())
                .collect(Collectors.toList()));
    }
}
