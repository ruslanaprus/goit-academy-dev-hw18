package com.example.notemanager.api.controller;

import com.example.notemanager.api.model.dto.SignupResultMapper;
import com.example.notemanager.api.model.dto.request.UserCreateRequest;
import com.example.notemanager.api.model.dto.request.UserLoginRequest;
import com.example.notemanager.api.model.dto.response.LoginResponse;
import com.example.notemanager.api.model.dto.response.SignupResponse;
import com.example.notemanager.security.CustomUserDetails;
import com.example.notemanager.model.User;
import com.example.notemanager.service.UserService;
import com.example.notemanager.api.util.JwtUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

        try {
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    request.userName(), request.password());
            Authentication authResult = authenticationManager.authenticate(authToken);

            // Get the authenticated user's details
            CustomUserDetails customUserDetails = (CustomUserDetails) authResult.getPrincipal();
            User user = customUserDetails.getUser();

            // Check account status
            if (userService.isAccountLocked(user)) {
                throw new ResponseStatusException(HttpStatus.LOCKED, "Account is locked. Try again later.");
            }

            // Set the authentication in the SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authResult);

            // Reset failed attempts using the existing User object
            userService.resetFailedAttempts(user);

            // Generate JWT Token
            String token = jwtUtil.generateToken(user);

            log.info("Authentication successful for user: {}", user.getUsername());
            return new LoginResponse(token);

        } catch (BadCredentialsException ex) {
            log.warn("Invalid credentials for user: {}", request.userName());
            // Record the failed attempt
            userService.findByUserName(request.userName()).ifPresent(user -> {
                userService.recordFailedAttempt(user.getId());
                if (userService.isAccountLocked(user)) {
                    log.warn("User {} is now locked due to too many failed attempts.", user.getUsername());
                    throw new ResponseStatusException(HttpStatus.LOCKED, "User is locked. Try again later.");
                }
            });
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
    }

}