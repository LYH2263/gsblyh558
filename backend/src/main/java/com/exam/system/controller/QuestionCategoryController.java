package com.exam.system.controller;

import com.exam.system.entity.QuestionCategory;
import com.exam.system.exception.ResourceNotFoundException;
import com.exam.system.repository.QuestionCategoryRepository;
import com.exam.system.payload.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/categories")
@Slf4j
public class QuestionCategoryController {

    @Autowired
    QuestionCategoryRepository categoryRepository;

    @GetMapping
    public ApiResponse<List<QuestionCategory>> getAllCategories() {
        return ApiResponse.success(categoryRepository.findAll());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<QuestionCategory> createCategory(@Valid @RequestBody QuestionCategory category) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("Admin [{}] is creating a new category: {}", authentication.getName(), category.getName());
        QuestionCategory savedCategory = categoryRepository.save(category);
        log.info("Category created successfully with ID: {}", savedCategory.getId());
        return ApiResponse.success(savedCategory);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<QuestionCategory> updateCategory(@PathVariable Long id, @Valid @RequestBody QuestionCategory categoryRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("Admin [{}] is updating category [ID: {}]", authentication.getName(), id);
        return ApiResponse.success(categoryRepository.findById(id).map(category -> {
            category.setName(categoryRequest.getName());
            category.setDescription(categoryRequest.getDescription());
            category.setParentId(categoryRequest.getParentId());
            QuestionCategory updatedCategory = categoryRepository.save(category);
            log.info("Category [ID: {}] updated successfully", id);
            return updatedCategory;
        }).orElseThrow(() -> new ResourceNotFoundException("未找到 ID 为 " + id + " 的分类")));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteCategory(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("Admin [{}] is deleting category [ID: {}]", authentication.getName(), id);
        categoryRepository.deleteById(id);
        log.info("Category [ID: {}] deleted successfully", id);
        return ApiResponse.success("分类删除成功", null);
    }
}
