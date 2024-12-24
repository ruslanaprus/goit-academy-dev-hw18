package com.example.notemanager.api.controller;

import com.example.notemanager.api.model.dto.SignupResultMapper;
import com.example.notemanager.api.model.dto.request.UserCreateRequest;
import com.example.notemanager.api.model.dto.request.UserLoginRequest;
import com.example.notemanager.api.model.dto.response.LoginResponse;
import com.example.notemanager.api.model.dto.response.SignupResponse;
import com.example.notemanager.model.User;
import com.example.notemanager.service.UserService;
import com.example.notemanager.api.util.JwtUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1")
public class AuthApiController {
    private static final Logger log = LoggerFactory.getLogger(AuthApiController.class);

    private final UserService userService;
    private final DaoAuthenticationProvider authenticationManager;
    private final JwtUtil jwtUtil;
    private final SignupResultMapper signupResultMapper;

    public AuthApiController(UserService userService,
                             DaoAuthenticationProvider authenticationManager,
                             JwtUtil jwtUtil,
                             SignupResultMapper signupResultMapper) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.signupResultMapper = signupResultMapper;
    }

    @PostMapping("/signup")
    public SignupResponse signup(@RequestBody UserCreateRequest request) {
        try {
            String message = userService.createUser(request.userName(), request.password());
            return signupResultMapper.toResponse(request.userName(), message);
        } catch (Exception e) {
            return signupResultMapper.toResponse(null, "Failed to create user: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody UserLoginRequest request) {
        log.info("Login request for user: {}", request.userName());

        // Fetch user once
        User user = userService.findByUserName(request.userName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        // Step 1: Check if account is locked
        if (userService.isAccountLocked(user)) {
            log.warn("Account is locked for user: {}", user.getUsername());
            throw new ResponseStatusException(HttpStatus.LOCKED, "Account is locked. Try again later.");
        }

        // Step 2: Attempt authentication
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(), request.password())
            );
        } catch (BadCredentialsException ex) {
            log.warn("Invalid credentials for user: {}", user.getUsername());
            userService.recordFailedAttempt(user.getId());

            if (userService.isAccountLocked(user)) {
                log.warn("Account locked for user: {}", user.getUsername());
                throw new ResponseStatusException(HttpStatus.LOCKED, "Account is locked. Try again later.");
            }

            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        // Step 3: Reset failed attempts only if conditions are met
        if (user.getFailedAttempts() > 0 && user.getFailedAttempts() <= 2) {
            userService.resetFailedAttempts(user.getId());
        }

        // Step 4: Generate JWT Token
        LoginResponse response = new LoginResponse(jwtUtil.generateToken(user));
        log.info("Authentication successful for user: {}", user.getUsername());
        return response;
    }

}