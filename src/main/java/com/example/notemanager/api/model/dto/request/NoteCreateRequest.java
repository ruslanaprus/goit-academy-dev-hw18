package com.example.notemanager.api.model.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record NoteCreateRequest(
        @NotNull(message = "Title must not be null")
        @NotEmpty(message = "Title must not be empty") String title,

        @NotNull(message = "Content must not be null")
        @NotEmpty(message = "Content must not be empty") String content) {}