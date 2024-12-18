package com.example.notemanager.controller;

import com.example.notemanager.exception.ExceptionMessages;
import com.example.notemanager.model.Note;
import com.example.notemanager.model.User;
import com.example.notemanager.model.dto.response.NoteResponse;
import com.example.notemanager.mvc.controller.NoteMvcController;
import com.example.notemanager.service.NoteService;
import com.example.notemanager.service.UserService;
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

    private User mockUser;

    @BeforeEach
    void setUp() {
        Mockito.reset(noteService);

        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUserName("mockUser");
        when(userService.getAuthenticatedUser()).thenReturn(mockUser);
    }

    @Test
    void listAllWhenNotesExist() throws Exception {
        NoteResponse note1 = new NoteResponse("title 1", "content 1");
        NoteResponse note2 = new NoteResponse("title 1", "content 1");
        NoteResponse note3 = new NoteResponse("title 1", "content 1");

        int page = 0;
        int size = 2;
        PageRequest pageRequest = PageRequest.of(page, size);
        // simulate behaviour of a Page: 2 notes on the page, pagination parameters, 3 - total number of items
        Page<NoteResponse> notePage = new PageImpl<>(List.of(note1, note2), pageRequest, 3);

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
        Note noteToUpdate = Note.builder().id(1L).title("Updated Title").content("Updated Content").build();

        NoteResponse updatedNoteResponse = NoteResponse.builder().title("Updated Title").content("Updated Content").build();

        when(noteService.update(any(Note.class))).thenReturn(updatedNoteResponse);

        mockMvc.perform(post("/note/edit")
                        .flashAttr("note", noteToUpdate)
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
        Note noteToCreate = Note.builder().title("New Title").content("New Content").build();

        NoteResponse createdNoteResponse = NoteResponse.builder().title("New Title").content("New Content").build();

        when(noteService.create(any(Note.class))).thenReturn(createdNoteResponse);

        mockMvc.perform(post("/note/create")
                        .flashAttr("note", noteToCreate)
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