package com.example.notemanager.api.model.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record UserCreateRequest(
        @NotNull(message = "Username must not be null")
        @NotEmpty(message = "Username must not be empty") String userName,

        @NotNull(message = "Password must not be null")
        @NotEmpty(message = "Password must not be empty") String password) {}