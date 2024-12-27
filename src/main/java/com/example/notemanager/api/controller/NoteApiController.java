package com.example.notemanager.api.controller;

import com.example.notemanager.api.model.dto.Mapper;
import com.example.notemanager.api.model.dto.request.NoteCreateRequest;
import com.example.notemanager.api.model.dto.request.NoteUpdateRequest;
import com.example.notemanager.model.Note;
import com.example.notemanager.api.model.dto.response.NoteResponse;
import com.example.notemanager.service.NoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    private final Mapper<NoteCreateRequest, Note> noteCreateRequestMapper;
    private final Mapper<NoteUpdateRequest, Note> noteUpdateRequestMapper;
    private final Mapper <Note, NoteResponse> noteMapper;

    @Operation(summary = "Display the list of notes",
            description = """
                    Retrieve a paginated list of all notes belonging to the authenticated user.
                    
                    **Pagination Parameters:**
                    - `page` (optional, default: `0`): The page number (zero-based index) to retrieve.
                    - `size` (optional, default: `10`): The number of notes per page.
                    
                    **Example Request:**
                    `GET http://localhost:8080/api/v1/notes?page=0&size=10`
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful operation"),
                    @ApiResponse(responseCode = "403", description = "User does not have permission to access this resource")
            })
    @GetMapping()
    public Page<NoteResponse> listAll(@RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "10") int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        return noteService.listAll(pageRequest)
                .map(noteMapper::map);
    }

    @Operation(summary = "Find a note by id",
            description = "Retrieve the details of a specific note using its unique identifier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = NoteResponse.class))}),
            @ApiResponse(responseCode = "400", description = "Invalid note ID provided",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "User does not have permission to access this resource",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Note not found",
                    content = @Content)
    })
    @GetMapping("/{id}")
    public NoteResponse getById(@PathVariable @Positive Long id) {
        Note note = noteService.getById(id);
        return noteMapper.map(note);
    }

    @Operation(summary = "Delete a note by id",
            description = "Remove a specific note using its unique identifier. The operation is irreversible")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Note successfully deleted with no response body"),
            @ApiResponse(responseCode = "400", description = "Invalid note ID provided"),
            @ApiResponse(responseCode = "403", description = "User does not have permission to access this resource"),
            @ApiResponse(responseCode = "404", description = "Note not found")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @Positive Long id) {
        noteService.delete(id);
    }

    @Operation(
            summary = "Edit a note by ID",
            description = "Update the details of an existing note using its unique identifier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully updated the note",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = NoteResponse.class))}),
            @ApiResponse(responseCode = "400", description = "Invalid note data or ID provided",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "User does not have permission to access this resource",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Note not found",
                    content = @Content)
    })
    @PutMapping("/{id}")
    public NoteResponse edit(@PathVariable @Positive Long id,
                             @Valid @RequestBody Note note) {
        note.setId(id);
        Note updatedNote = noteService.update(note);
        return noteMapper.map(updatedNote);
    }

    @Operation(
            summary = "Create a new note",
            description = "Add a new note to the system for the authenticated user",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "New note details",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = NoteCreateRequest.class),
                            examples = @ExampleObject(value = "{ \"title\": \"My new note\", \"content\": \"Practiced my cutest meow. Human was impressed\"}"))
            ))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Successfully created the note",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = NoteResponse.class))}),
            @ApiResponse(responseCode = "400", description = "Invalid note data provided",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "User does not have permission to access this resource",
                    content = @Content)
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NoteResponse create(@Valid @RequestBody NoteCreateRequest newNote) {
        Note savedNote = noteService.create(noteCreateRequestMapper.map(newNote));
        return noteMapper.map(savedNote);
    }

    @Operation(
            summary = "Search for a note by a keyword",
            description = """
                    Search for notes containing the specified keyword in their title or content.
                    
                    **Request Parameters:**
                    - `keyword` (required): The search term to look for in the notes. This parameter cannot be empty.
                    - `page` (optional, default: `0`): The page number (zero-based index) to retrieve for the search results.
                    - `size` (optional, default: `10`): The number of notes per page in the search results.
                    
                    **Example Request:**
                    `GET http://localhost:8080/api/v1/notes/search?keyword=cute&page=0&size=10`
                    
                    This will search for notes containing the word "cute" and return the first page of results with up to 10 notes per page.
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved the search results"),
                    @ApiResponse(responseCode = "400", description = "Invalid search query provided"),
                    @ApiResponse(responseCode = "403", description = "User does not have permission to access this resource")
            })
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