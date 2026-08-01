package com.exam.system.service;

import com.exam.system.entity.Question;
import com.exam.system.entity.QuestionType;
import com.exam.system.exception.ResourceNotFoundException;
import com.exam.system.repository.QuestionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class QuestionService {
    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    public List<Question> getAllQuestions() {
        return questionRepository.findAll();
    }

    public Question getQuestionById(Long id) {
        return questionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Question not found"));
    }

    public Question createQuestion(Question question) {
        return questionRepository.save(question);
    }

    public Question updateQuestion(Long id, Question questionDetails) {
        Question question = getQuestionById(id);
        question.setCategory(questionDetails.getCategory());
        question.setType(questionDetails.getType());
        question.setContent(questionDetails.getContent());
        question.setOptions(questionDetails.getOptions());
        question.setAnswer(questionDetails.getAnswer());
        question.setAnalysis(questionDetails.getAnalysis());
        question.setDifficulty(questionDetails.getDifficulty());
        return questionRepository.save(question);
    }

    public void deleteQuestion(Long id) {
        questionRepository.deleteById(id);
    }

    public boolean isAnswerCorrect(Question question, String userAnswer) {
        if (userAnswer == null) {
            log.debug("User answer is null for question [ID: {}]", question.getId());
            return false;
        }
        
        String trimmedUserAnswer = userAnswer.trim();
        String correctAnswer = question.getAnswer();

        if (question.getType() == QuestionType.SHORT_ANSWER) {
            try {
                JsonNode root = objectMapper.readTree(correctAnswer);
                if (root.has("keywords")) {
                    JsonNode keywords = root.get("keywords");
                    double totalWeight = 0;
                    double earnedWeight = 0;
                    
                    log.debug("Evaluating short answer for question [ID: {}]. User answer: [{}]", question.getId(), trimmedUserAnswer);
                    
                    for (JsonNode keywordNode : keywords) {
                        String keyword = keywordNode.get("text").asText();
                        double weight = keywordNode.get("weight").asDouble();
                        totalWeight += weight;
                        
                        boolean contains = trimmedUserAnswer.toLowerCase().contains(keyword.toLowerCase());
                        
                        if (contains) {
                            earnedWeight += weight;
                            log.debug("Keyword [{}] matched. Earned weight: {}", keyword, weight);
                        }
                    }
                    
                    double scoreRatio = totalWeight > 0 ? (earnedWeight / totalWeight) : 0;
                    boolean isCorrect = scoreRatio >= 0.6;
                    log.debug("Short answer result for question [ID: {}]: ratio={}, isCorrect={}", question.getId(), scoreRatio, isCorrect);
                    return isCorrect;
                }
            } catch (Exception e) {
                log.error("Error parsing short answer JSON for question [ID: {}]: {}", question.getId(), e.getMessage());
                return trimmedUserAnswer.equalsIgnoreCase(correctAnswer.trim());
            }
        }

        // Default comparison for other types
        return trimmedUserAnswer.equalsIgnoreCase(correctAnswer.trim());
    }
}
