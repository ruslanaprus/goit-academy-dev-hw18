package com.example.notemanager.api.controller;

import com.example.notemanager.api.model.dto.SignupResultMapper;
import com.example.notemanager.api.model.dto.request.UserCreateRequest;
import com.example.notemanager.api.model.dto.request.UserLoginRequest;
import com.example.notemanager.api.model.dto.response.LoginResponse;
import com.example.notemanager.api.model.dto.response.SignupResponse;
import com.example.notemanager.model.User;
import com.example.notemanager.service.UserService;
import com.example.notemanager.api.util.JwtUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1")
@Tags(value = {
        @Tag(name = "Authentication controller", description = "Manages user authentication and account creation for the application")
}
)
public class AuthApiController {
    private static final Logger log = LoggerFactory.getLogger(AuthApiController.class);

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final SignupResultMapper signupResultMapper;
    private final PasswordEncoder passwordEncoder;

    public AuthApiController(UserService userService,
                             JwtUtil jwtUtil,
                             SignupResultMapper signupResultMapper,
                             @Qualifier("passEncoder") PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.signupResultMapper = signupResultMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Operation(summary = "Sign up", description = "Add a new user",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "User registration details",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserCreateRequest.class))
            ))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SignupResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input provided",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content)
    })
    @PostMapping("/signup")
    public SignupResponse signup(@Valid @RequestBody UserCreateRequest request) {
        try {
            String message = userService.createUser(request.userName(), request.password());
            return signupResultMapper.toResponse(request.userName(), message);
        } catch (Exception e) {
            return signupResultMapper.toResponse(null, "Failed to create user: " + e.getMessage());
        }
    }

    @Operation(summary = "Login", description = "Authenticate a user and return a token",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "User login credentials",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserLoginRequest.class))
            ))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful - returns authentication token",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input provided",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorised - Invalid credentials",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content),
            @ApiResponse(responseCode = "423", description = "Account locked",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content)
    })
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody UserLoginRequest request) {
        String username = request.userName();
        String password = request.password();

        Optional<User> userOpt = userService.findByUserName(username);
        if (userOpt.isEmpty()) {
            userService.evictUserFromCache(username);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        User user = userOpt.get();

        // Check if the account is locked
        if (userService.isAccountLocked(user)) {
            throw new ResponseStatusException(HttpStatus.LOCKED, "User is locked. Try again later.");
        }

        // Verify password
        if (!passwordEncoder.matches(password, user.getPassword())) {
            userService.recordFailedAttempt(user.getId());
            userService.evictUserFromCache(username);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        // Reset failed attempts if necessary
        userService.resetFailedAttempts(user);

        // Cache user details
        userService.cacheUser(username, user);

        // Generate JWT or session token (assuming JWT here)
        String token = jwtUtil.generateToken(user);
        log.info("user logged in: {}", username);
        return new LoginResponse(token);
    }

}