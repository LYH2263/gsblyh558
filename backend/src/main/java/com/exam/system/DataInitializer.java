package com.exam.system;

import com.exam.system.entity.*;
import com.exam.system.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QuestionCategoryRepository categoryRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private ExamQuestionRepository examQuestionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        initUsers();
    }

    private void initUsers() {
        if (userRepository.count() == 0) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("123456"));
            admin.setEmail("admin@example.com");
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);

            User user = new User();
            user.setUsername("user");
            user.setPassword(passwordEncoder.encode("123456"));
            user.setEmail("user@example.com");
            user.setRole(Role.USER);
            userRepository.save(user);
            
            System.out.println("Users initialized: admin/123456, user/123456");
        }
    }
}
