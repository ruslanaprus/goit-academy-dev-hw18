package com.example.notemanager.service;

import com.example.notemanager.exception.EntityException;
import com.example.notemanager.exception.ExceptionMessages;
import com.example.notemanager.model.User;
import com.example.notemanager.security.UserContext;
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
    private final UserContext userContext;

    public UserService(UserRepository userRepository,
                       @Qualifier("passEncoder") PasswordEncoder passwordEncoder, UserContext userContext) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userContext = userContext;
    }

    public User getAuthenticatedUser() {
        if (userContext.getCachedUser() != null) {
            return userContext.getCachedUser();
        }
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new EntityException(ExceptionMessages.USER_NOT_FOUND.getMessage()));
        userContext.setCachedUser(user);
        return user;
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
    public void resetFailedAttempts(User user) {
        if (user.getFailedAttempts() > 0 || user.getAccountLockedUntil() != null) {
            userRepository.resetFailedAttempts(user.getId());
        }
    }
}
