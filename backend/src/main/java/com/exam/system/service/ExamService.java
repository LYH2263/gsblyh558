package com.exam.system.service;

import com.exam.system.entity.Exam;
import com.exam.system.exception.ResourceNotFoundException;
import com.exam.system.repository.ExamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExamService {
    @Autowired
    private ExamRepository examRepository;

    public List<Exam> getAllExams() {
        return examRepository.findAll();
    }

    public Exam createExam(Exam exam) {
        return examRepository.save(exam);
    }
    
    public Exam getExamById(Long id) {
        return examRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Exam not found"));
    }
}
