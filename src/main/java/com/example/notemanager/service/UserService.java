package com.example.notemanager.service;

import com.example.notemanager.exception.EntityException;
import com.example.notemanager.exception.ExceptionMessages;
import com.example.notemanager.model.User;
import com.example.notemanager.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private static final int MAX_FAILED_ATTEMPTS = 3;
    private static final int LOCK_DURATION_MINUTES = 15;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       @Qualifier("passEncoder") PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User getAuthenticatedUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUserName(username)
                .orElseThrow(() -> new EntityException(ExceptionMessages.ENTITY_NOT_FOUND.getMessage()));
    }

    public String createUser(String username, String password) {
        if (userRepository.existsByUserName(username)) {
            return "User already exists";
        }

        User user = User.builder()
                .userName(username)
                .password(passwordEncoder.encode(password))
                .role("ROLE_USER")
                .build();
        userRepository.save(user);
        return "User created";
    }

    public Optional<User> findByUserName(String userName) {
        return Optional.ofNullable(userRepository.findByUserName(userName).orElseThrow(() ->
                new EntityException(ExceptionMessages.ENTITY_NOT_FOUND.getMessage())));
    }

    public boolean isAccountLocked(User user) {
        boolean isLocked = user.getAccountLockedUntil() != null &&
                user.getAccountLockedUntil().isAfter(LocalDateTime.now());
        if (isLocked) {
            log.warn("User {} is locked until {}", user.getUsername(), user.getAccountLockedUntil());
        }
        return isLocked;
    }

    @Transactional
    public void recordFailedAttempt(Long userId) {
        LocalDateTime lockTime = LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES);
        userRepository.incrementFailedAttempts(userId, MAX_FAILED_ATTEMPTS, lockTime);
    }

    @Transactional
    public void resetFailedAttempts(Long userId) {
        userRepository.resetFailedAttempts(userId);
    }
}
