package com.example.notemanager.config;

import com.example.notemanager.exception.EntityException;
import com.example.notemanager.exception.ExceptionMessages;
import com.example.notemanager.security.CustomUserDetails;
import com.example.notemanager.model.User;
import com.example.notemanager.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecurityConfig {
    private final UserRepository userRepository;

    public SecurityConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Bean(name = "passEncoder")
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean(name = "userDetails")
    public UserDetailsService userDetailsService() {
        return username -> {
            User user = userRepository.findByUserName(username)
                    .orElseThrow(() -> new EntityException(ExceptionMessages.USER_NOT_FOUND.getMessage()));
            return new CustomUserDetails(user);
        };
    }
}
