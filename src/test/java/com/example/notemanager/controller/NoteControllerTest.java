package com.example.notemanager.controller;

import com.example.notemanager.exception.ExceptionMessages;
import com.example.notemanager.model.Note;
import com.example.notemanager.model.User;
import com.example.notemanager.mvc.controller.NoteMvcController;
import com.example.notemanager.service.NoteService;
import com.example.notemanager.service.UserService;
import com.github.benmanes.caffeine.cache.Cache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NoteMvcController.class)
class NoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NoteService noteService;

    @MockBean
    private UserService userService;

    @MockBean
    private Cache<String, User> userCache;

    @BeforeEach
    void setUp() {
        Mockito.reset(noteService, userService);

        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUserName("mockUser");

        when(userService.getAuthenticatedUser()).thenReturn(mockUser);
        when(userService.findByUserName("mockUser")).thenReturn(Optional.of(mockUser));
    }

    @Test
    void listAllWhenNotesExist() throws Exception {
        Note note1 = Note.builder().id(1L).title("title 1").content("content 1").build();
        Note note2 = Note.builder().id(2L).title("title 2").content("content 2").build();
        Note note3 = Note.builder().id(3L).title("title 3").content("content 3").build();

        int page = 0;
        int size = 2;
        PageRequest pageRequest = PageRequest.of(page, size);
        // simulate behaviour of a Page: 2 notes on the page, pagination parameters, 3 - total number of items
        Page<Note> notePage = new PageImpl<>(List.of(note1, note2), pageRequest, 3);

        when(noteService.listAll(pageRequest)).thenReturn(notePage);

        mockMvc.perform(get("/note/list")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .with(user("mockUser").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("note/list"))
                .andExpect(model().attributeExists("notes", "username", "currentPage", "totalPages", "totalItems", "size"))
                .andExpect(model().attribute("notes", List.of(note1, note2)))
                .andExpect(model().attribute("username", "mockUser"))
                .andExpect(model().attribute("currentPage", page))
                .andExpect(model().attribute("totalPages", 2))
                .andExpect(model().attribute("totalItems", 3L))
                .andExpect(model().attribute("size", size));
    }

    @Test
    void editValidIdReturnsEditView() throws Exception {
        Note note = Note.builder().id(1L).title("initial title").content("initial content").build();
        when(noteService.getById(1L)).thenReturn(note);

        mockMvc.perform(get("/note/edit")
                        .param("id", "1")
                        .with(user("mockUser").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("note/edit"))
                .andExpect(model().attributeExists("note"))
                .andExpect(model().attribute("note", note));

        verify(noteService, times(1)).getById(1L);
    }

    @Test
    void editByIdUpdatesNoteAndRedirectsToList() throws Exception {
        Note updatedNote = Note.builder().id(1L).title("Updated Title").content("Updated Content").build();

        when(noteService.update(any(Note.class))).thenReturn(updatedNote);

        mockMvc.perform(post("/note/edit")
                        .flashAttr("note", updatedNote)
                        .with(csrf())
                        .with(user("mockUser").roles("USER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/note/list"));

        verify(noteService, times(1)).update(any(Note.class));
    }

    @Test
    void editByIdWithInvalidNoteThrowsException() throws Exception {
        Note invalidNote = Note.builder().id(1L).title("").content("").build();

        mockMvc.perform(post("/note/edit")
                        .flashAttr("note", invalidNote)
                        .with(csrf())
                        .with(user("mockUser").roles("USER")))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("note/error"))
                .andExpect(model().attributeExists("message"))
                .andExpect(model().attribute("message", ExceptionMessages.INVALID_NOTE_DATA.getMessage()));

        verify(noteService, never()).update(any(Note.class));
    }

    @Test
    void deleteValidIdRedirectsToList() throws Exception {
        doNothing().when(noteService).delete(1L);

        mockMvc.perform(post("/note/delete")
                        .param("id", "1")
                        .with(csrf())
                        .with(user("mockUser").roles("USER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/note/list"));

        verify(noteService, times(1)).delete(1L);
    }

    @Test
    void createFormReturnsCreateView() throws Exception {
        mockMvc.perform(get("/note/create")
                        .with(csrf())
                        .with(user("mockUser").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("note/create"));
    }

    @Test
    void createNewNoteRedirectsToList() throws Exception {
        Note newNote = Note.builder().title("new title").content("new content").build();

        when(noteService.create(any(Note.class))).thenReturn(newNote);

        mockMvc.perform(post("/note/create")
                        .flashAttr("note", newNote)
                        .with(csrf())
                        .with(user("mockUser").roles("USER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/note/list"));

        verify(noteService, times(1)).create(any(Note.class));
    }

    @Test
    void createNewNoteWithInvalidDataThrowsException() throws Exception {
        Note invalidNote = Note.builder().title("").content("").build();

        mockMvc.perform(post("/note/create")
                        .flashAttr("note", invalidNote)
                        .with(csrf())
                        .with(user("mockUser").roles("USER")))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("note/error"))
                .andExpect(model().attributeExists("message"))
                .andExpect(model().attribute("message", ExceptionMessages.INVALID_NOTE_DATA.getMessage()));
    }
}