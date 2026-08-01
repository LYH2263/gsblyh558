package com.exam.system.service;

import com.exam.system.entity.QuestionCategory;
import com.exam.system.exception.ResourceNotFoundException;
import com.exam.system.repository.QuestionCategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuestionCategoryService {
    @Autowired
    private QuestionCategoryRepository categoryRepository;

    public List<QuestionCategory> getAllCategories() {
        return categoryRepository.findAll();
    }

    public QuestionCategory createCategory(QuestionCategory category) {
        return categoryRepository.save(category);
    }

    public QuestionCategory updateCategory(Long id, QuestionCategory categoryDetails) {
        QuestionCategory category = categoryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        category.setName(categoryDetails.getName());
        category.setDescription(categoryDetails.getDescription());
        category.setParentId(categoryDetails.getParentId());
        return categoryRepository.save(category);
    }

    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }
}
