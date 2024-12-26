package com.example.notemanager.api.controller;

import com.example.notemanager.model.Note;
import com.example.notemanager.api.model.dto.NoteMapper;
import com.example.notemanager.api.model.dto.response.NoteResponse;
import com.example.notemanager.service.NoteService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notes")
@RequiredArgsConstructor
@Tags(value = {
        @Tag(name = "Note controller", description = "Provides operations for managing notes")
}
)
public class NoteApiController {

    private final NoteService noteService;
    private final NoteMapper noteMapper;

    @GetMapping()
    public Page<NoteResponse> listAll(@RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "10") int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        return noteService.listAll(pageRequest)
                .map(noteMapper::toResponse);
    }

    @GetMapping("/{id}")
    public NoteResponse getById(@PathVariable @Positive Long id) {
        Note note = noteService.getById(id);
        return noteMapper.toResponse(note);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @Positive Long id) {
        noteService.delete(id);
    }

    @PutMapping("/{id}")
    public NoteResponse edit(@PathVariable @Positive Long id,
                                     @Valid @RequestBody Note note) {
        note.setId(id);
        Note updatedNote = noteService.update(note);
        return noteMapper.toResponse(updatedNote);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NoteResponse create(@Valid @RequestBody Note note) {
        Note savedNote = noteService.create(note);
        return noteMapper.toResponse(savedNote);
    }

    @GetMapping("/search")
    public Page<NoteResponse> searchNotes(@RequestParam String keyword,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "10") int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        return noteService.search(keyword, pageRequest)
                .map(note -> NoteResponse.builder()
                        .title(note.getTitle())
                        .content(note.getContent())
                        .build());
    }
}