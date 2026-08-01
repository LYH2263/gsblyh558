package com.exam.system.controller;

import com.exam.system.entity.Question;
import com.exam.system.exception.ResourceNotFoundException;
import com.exam.system.repository.QuestionRepository;
import com.exam.system.payload.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/questions")
@Slf4j
public class QuestionController {

    @Autowired
    QuestionRepository questionRepository;

    @GetMapping
    public ApiResponse<Page<Question>> getAllQuestions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "id") String sortField,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        // Limit page size to prevent excessive data fetching
        if (size > 100) {
            size = 100;
        }

        if ("random".equalsIgnoreCase(sortField) && categoryId != null) {
            List<Question> randomQuestions = questionRepository.findRandomByCategoryId(categoryId, size);
            // Use PageRequest without sort for the PageImpl wrapper
            return ApiResponse.success(new PageImpl<>(randomQuestions, PageRequest.of(page, size), randomQuestions.size()));
        }

        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortField).descending() : Sort.by(sortField).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        if (categoryId != null) {
            return ApiResponse.success(questionRepository.findByCategoryId(categoryId, pageable));
        }
        return ApiResponse.success(questionRepository.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<Question> getQuestion(@PathVariable Long id) {
        return ApiResponse.success(questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("未找到 ID 为 " + id + " 的题目")));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Question> createQuestion(@Valid @RequestBody Question question) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("Admin [{}] is creating a new question of type [{}]", authentication.getName(), question.getType());
        Question savedQuestion = questionRepository.save(question);
        log.info("Question created successfully with ID: {}", savedQuestion.getId());
        return ApiResponse.success(savedQuestion);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Question> updateQuestion(@PathVariable Long id, @Valid @RequestBody Question questionRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("Admin [{}] is updating question [ID: {}]", authentication.getName(), id);
        return ApiResponse.success(questionRepository.findById(id).map(question -> {
            question.setCategory(questionRequest.getCategory());
            question.setType(questionRequest.getType());
            question.setContent(questionRequest.getContent());
            question.setOptions(questionRequest.getOptions());
            question.setAnswer(questionRequest.getAnswer());
            question.setAnalysis(questionRequest.getAnalysis());
            question.setDifficulty(questionRequest.getDifficulty());
            Question updatedQuestion = questionRepository.save(question);
            log.info("Question [ID: {}] updated successfully", id);
            return updatedQuestion;
        }).orElseThrow(() -> new ResourceNotFoundException("未找到 ID 为 " + id + " 的题目")));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteQuestion(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("Admin [{}] is deleting question [ID: {}]", authentication.getName(), id);
        questionRepository.deleteById(id);
        log.info("Question [ID: {}] deleted successfully", id);
        return ApiResponse.success("题目删除成功", null);
    }
}
