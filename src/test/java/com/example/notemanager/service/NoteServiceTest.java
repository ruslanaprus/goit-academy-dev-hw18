package com.example.notemanager.service;

import com.example.notemanager.exception.ExceptionMessages;
import com.example.notemanager.exception.NoteServiceException;
import com.example.notemanager.model.Note;
import com.example.notemanager.model.User;
import com.example.notemanager.repository.NoteRepository;
import com.github.benmanes.caffeine.cache.Cache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NoteServiceTest {
    private NoteService noteService;
    private NoteRepository noteRepository;
    private User mockUser;
    private UserService userService;
    private Cache<String, User> userCache;

    @BeforeEach
    void setUp() {
        noteRepository = mock(NoteRepository.class);
        userService = mock(UserService.class);
        userCache = mock(Cache.class);

        noteService = new NoteService(noteRepository, userService);

        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUserName("testuser");
        when(userService.getAuthenticatedUser()).thenReturn(mockUser);
    }

    @Test
    void listAllReturnsEmptyListWhenNoNotesExist() {
        PageRequest pageRequest = PageRequest.of(0, 5);
        Page<Note> emptyPage = Page.empty(pageRequest);

        when(noteRepository.findByUser(mockUser, pageRequest)).thenReturn(emptyPage);

        Page<Note> result = noteService.listAll(pageRequest);

        assertNotNull(result, "Result should not be null.");
        assertTrue(result.isEmpty(), "Expected no notes in the page.");
    }

    @Test
    void listAllReturnsAllExistingNotes() {
        Note note1 = Note.builder().id(1L).title("title 1").content("content 1").build();
        Note note2 = Note.builder().id(2L).title("title 2").content("content 2").build();
        Note note3 = Note.builder().id(3L).title("title 3").content("content 3").build();

        int page = 0;
        int size = 2;
        PageRequest pageRequest = PageRequest.of(page, size);
        // simulate behaviour of a Page: 2 notes on the page, pagination parameters, 3 - total number of items
        Page<Note> notePage = new PageImpl<>(List.of(note1, note2), pageRequest, 3);

        when(noteRepository.findByUser(mockUser, pageRequest)).thenReturn(notePage);

        Page<Note> result = noteService.listAll(pageRequest);

        assertNotNull(result, "Result should not be null.");
        assertEquals(2, result.getContent().size(), "The page should contain the correct number of notes.");
        assertTrue(result.getContent().contains(note1), "The page should contain note1.");
        assertTrue(result.getContent().contains(note2), "The page should contain note2.");
        assertEquals(3, result.getTotalElements(), "Total elements should match the expected count.");
        assertEquals(2, result.getTotalPages(), "Total pages should match the expected count.");
        assertEquals(page, result.getNumber(), "Current page number should match the requested page.");
    }

    @Test
    void createSavesAndReturnsNewNote() {
        Note inputNote = Note.builder().title("title").content("content").build();
        Note savedNote = Note.builder().id(1L).title("title").content("content").build();

        when(noteRepository.save(inputNote)).thenReturn(savedNote);

        Note result = noteService.create(inputNote);

        assertNotNull(result);
        assertEquals(savedNote, result, "The saved note should match the returned note.");

        ArgumentCaptor<Note> captor = ArgumentCaptor.forClass(Note.class);
        verify(noteRepository).save(captor.capture());
        assertEquals(mockUser, captor.getValue().getUser());
    }

    @Test
    void createThrowsExceptionWhenTitleIsNullOrEmpty() {
        Note noteWithNullTitle = Note.builder().content("content").build();
        Note noteWithEmptyTitle = Note.builder().title("").content("content").build();

        assertThrows(NoteServiceException.class, () -> noteService.create(noteWithNullTitle));
        assertThrows(NoteServiceException.class, () -> noteService.create(noteWithEmptyTitle));
    }

    @Test
    void getByIdReturnsNoteIfExists() {
        Note note = Note.builder().id(1l).title("title").content("content").build();
        when(noteRepository.findByIdAndUser(1L, mockUser)).thenReturn(Optional.of(note));

        Note result = noteService.getById(1L);

        assertNotNull(result);
        assertEquals(note, result, "The returned note should match the existing note.");
    }

    @Test
    void getByIdThrowsExceptionIfNoteDoesNotExist() {
        when(noteRepository.findByIdAndUser(999L, mockUser)).thenReturn(Optional.empty());

        Exception exception = assertThrows(NoteServiceException.class, () -> noteService.getById(999L));
        assertEquals(ExceptionMessages.NOTE_NOT_FOUND.getMessage(), exception.getMessage());
    }

    @Test
    void updateSavesAndReturnsUpdatedNoteIfExists() {
        Note existingNote = Note.builder().id(1L).title("old title").content("old content").user(mockUser).build();
        Note updatedNote = Note.builder().id(1L).title("new title").content("new content").build();

        when(noteRepository.findByIdAndUser(1L, mockUser)).thenReturn(Optional.of(existingNote));
        when(noteRepository.save(existingNote)).thenReturn(existingNote);

        Note result = noteService.update(updatedNote);

        assertEquals(existingNote, result);
        assertEquals("new title", result.getTitle());
        assertEquals("new content", result.getContent());
    }

    @Test
    void updateThrowsIfNoteDoesNotExist() {
        Note nonExistentNote = Note.builder().id(999L).title("nonexistent").content("no content").build();

        when(noteRepository.findByIdAndUser(999L, mockUser)).thenReturn(Optional.empty());

        Exception exception = assertThrows(NoteServiceException.class, () -> noteService.update(nonExistentNote));
        assertEquals(ExceptionMessages.NOTE_NOT_FOUND.getMessage(), exception.getMessage());
    }

    @Test
    void deleteRemovesExistingNote() {
        Note note = Note.builder().id(1L).title("title").content("content").user(mockUser).build();

        when(noteRepository.findByIdAndUser(1L, mockUser)).thenReturn(Optional.of(note));

        noteService.delete(1L);

        verify(noteRepository).delete(note);
    }

    @Test
    void deleteThrowsIfNoteDoesNotExist() {
        when(noteRepository.findByIdAndUser(999L, mockUser)).thenReturn(Optional.empty());

        Exception exception = assertThrows(NoteServiceException.class, () -> noteService.delete(999L));
        assertEquals(ExceptionMessages.NOTE_NOT_FOUND.getMessage(), exception.getMessage());
    }

    @Test
    void userCachePopulatesOnAuthentication() {
        String username = mockUser.getUsername();
        when(userCache.getIfPresent(username)).thenReturn(null);

        userCache.put(username, mockUser);
        verify(userCache).put(username, mockUser);
    }
}
