package com.example.notemanager.api.config;

import com.example.notemanager.api.model.dto.Mapper;
import com.example.notemanager.api.model.dto.request.NoteCreateRequest;
import com.example.notemanager.api.model.dto.request.NoteUpdateRequest;
import com.example.notemanager.api.model.dto.response.NoteResponse;
import com.example.notemanager.model.Note;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MapperConfig {

    @Bean
    public Mapper<NoteCreateRequest, Note> noteCreateRequestMapper() {
        return request -> Note.builder()
                .title(request.title())
                .content(request.content())
                .build();
    }

    @Bean
    public Mapper<NoteUpdateRequest, Note> noteUpdateRequestMapper() {
        return request -> Note.builder()
                .title(request.title())
                .content(request.content())
                .build();
    }

    @Bean
    public Mapper<Note, NoteResponse> noteMapper() {
        return note -> NoteResponse.builder()
                .title(note.getTitle())
                .content(note.getContent())
                .build();
    }
}