package com.example.notemanager.api.model.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record UserLoginRequest(
        @NotNull(message = "Username must not be null")
        @NotEmpty(message = "Username must not be empty") String userName,

        @NotNull(message = "Password must not be null")
        @NotEmpty(message = "Password must not be empty") String password) {}