package com.example.notemanager.service;

import com.example.notemanager.exception.EntityException;
import com.example.notemanager.exception.ExceptionMessages;
import com.example.notemanager.model.User;
import com.example.notemanager.repository.UserRepository;
import com.github.benmanes.caffeine.cache.Cache;
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
    private final Cache<String, User> userCache;

    public UserService(UserRepository userRepository,
                       @Qualifier("passEncoder") PasswordEncoder passwordEncoder,
                       Cache<String, User> userCache) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userCache = userCache;
    }

    public User getAuthenticatedUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Fetching authenticated user for: {}", username);

        // Try to retrieve the user from the cache
        User cachedUser = userCache.getIfPresent(username);
        if (cachedUser != null) {
            log.info("User found in cache: {}", username);
            return cachedUser;
        }

        // Fallback: Retrieve the user from the database and store in cache
        log.info("User not found in cache: {}", username);
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new EntityException(ExceptionMessages.USER_NOT_FOUND.getMessage()));
        userCache.put(username, user);
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
        log.info("Incremented failed attempts");
    }

    @Transactional
    public void resetFailedAttempts(User user) {
        if (user.getFailedAttempts() > 0 || user.getAccountLockedUntil() != null) {
            userRepository.resetFailedAttempts(user.getId());
            log.info("Reset failed attempts");
        }
    }

    public void cacheUser(String username, User user) {
        log.info("Caching user {}", username);
        userCache.put(username, user);
    }

    public void evictUserFromCache(String username) {
        log.info("Evicting user {}", username);
        userCache.invalidate(username);
    }
}
