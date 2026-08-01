package com.exam.system.controller;

import com.exam.system.entity.Role;
import com.exam.system.entity.User;
import com.exam.system.payload.request.LoginRequest;
import com.exam.system.payload.request.SignupRequest;
import com.exam.system.payload.response.ApiResponse;
import com.exam.system.payload.response.JwtResponse;
import com.exam.system.payload.response.MessageResponse;
import com.exam.system.repository.UserRepository;
import com.exam.system.security.JwtUtils;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    @PostMapping("/signin")
    public ResponseEntity<ApiResponse<JwtResponse>> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Authenticating user: {}", loginRequest.getUsername());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());
        
        // Fetch email from DB as UserDetails might not have it populated depending on impl
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();

        log.info("User authenticated successfully: {}", user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(new JwtResponse(jwt,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                roles)));
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<MessageResponse>> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        log.info("Registering new user: {}", signUpRequest.getUsername());
        if (userRepository.findByUsername(signUpRequest.getUsername()).isPresent()) {
            log.warn("Registration failed: Username {} is already taken", signUpRequest.getUsername());
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.error(400, "错误：用户名已被占用！"));
        }

        // Create new user's account
        User user = new User();
        user.setUsername(signUpRequest.getUsername());
        user.setPassword(encoder.encode(signUpRequest.getPassword()));
        user.setEmail(signUpRequest.getEmail());
        
        if (signUpRequest.getRole() != null) {
            user.setRole(signUpRequest.getRole());
        } else {
            user.setRole(Role.USER);
        }

        userRepository.save(user);

        log.info("User registered successfully: {}", user.getUsername());
        return ResponseEntity.ok(ApiResponse.success("用户注册成功！", null));
    }
}
