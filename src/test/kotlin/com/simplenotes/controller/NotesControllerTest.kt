package com.simplenotes.controller

import com.simplenotes.controller.model.NoteRequestEntity
import com.simplenotes.exception.NoteException
import com.simplenotes.model.Note
import com.simplenotes.model.User
import com.simplenotes.service.NoteService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.time.Instant
import java.util.*

class NotesControllerTest {
    private val noteService: NoteService = mock()
    private val notesController = NotesController(noteService)

    private val user = User(UUID.randomUUID(), "test-user", "password")
    private val note = Note(1, 1, "test title", "This is a test note", Instant.now())

    @Test
    fun `createNote should return created note`() {
        val noteRequest = NoteRequestEntity("test title request", "request content")
        val note = Note(1, 1, noteRequest.title, noteRequest.content, Instant.now())

        given(noteService.createNote(user, noteRequest.title, noteRequest.content, null)) willReturn { note }

        val response = notesController.createNote(user, noteRequest)

        Assertions.assertEquals(HttpStatus.OK, response.statusCode)
        Assertions.assertEquals(note, response.body)
    }

    @Test
    fun `getNotes should return list of notes`() {
        val notes = listOf(note)
        val cursor = Instant.now()
        val limit = 10

        given(noteService.getNotes(user, cursor, limit)) willReturn { notes }

        val response: ResponseEntity<List<Note>> = notesController.getNotes(user, cursor, limit)

        Assertions.assertEquals(HttpStatus.OK, response.statusCode)
        Assertions.assertEquals(notes, response.body)
    }

    @Test
    fun `getNotes should throw NoteException when user ID is null`() {
        val userWithoutId = User(null, "test-user", "password")

        given(noteService.getNotes(userWithoutId, null, null)).willThrow(NoteException::class.java)

        assertThrows<NoteException> {
            notesController.getNotes(userWithoutId, null, null)
        }
    }

    @Test
    fun `getNote should return a note`() {
        val noteId = 1L

        given(noteService.getNote(user, noteId)) willReturn { note }

        val response: ResponseEntity<Note?> = notesController.getNote(user, noteId)

        Assertions.assertEquals(HttpStatus.OK, response.statusCode)
        Assertions.assertEquals(note, response.body)
    }

    @Test
    fun `getNote should return not found when note does not exist`() {
        val noteId = 999L

        given(noteService.getNote(user, noteId)) willReturn { null }

        val response: ResponseEntity<Note?> = notesController.getNote(user, noteId)

        Assertions.assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        Assertions.assertEquals(null, response.body)
    }

    @Test
    fun `getNote should throw NoteException when user ID is null`() {
        val userWithoutId = User(null, "test-user", "password")
        val noteId = 1L

        given(noteService.getNote(userWithoutId, noteId)).willThrow(NoteException::class.java)

        assertThrows<NoteException> {
            notesController.getNote(userWithoutId, noteId)
        }
    }

    @Test
    fun `getVersions should return list of note versions`() {
        val noteId = 1L
        val versions = listOf(note)
        val cursor = Instant.now()
        val limit = 10

        given(noteService.getVersions(user, noteId, cursor, limit)) willReturn { versions }

        val response: ResponseEntity<List<Note>> = notesController.getVersions(user, noteId, cursor, limit)

        Assertions.assertEquals(HttpStatus.OK, response.statusCode)
        Assertions.assertEquals(versions, response.body)
    }

    @Test
    fun `getVersions should throw NoteException when user ID is null`() {
        val userWithoutId = User(null, "test-user", "password")
        val noteId = 1L
        val cursor = Instant.now()
        val limit = 10

        given(noteService.getVersions(userWithoutId, noteId, cursor, limit)).willThrow(NoteException::class.java)

        assertThrows<NoteException> {
            notesController.getVersions(userWithoutId, noteId, cursor, limit)
        }
    }

    @Test
    fun `updateNote should return updated note`() {
        val noteId = 1L
        val updatedNote = note.copy(content = "Updated content")
        val noteRequest = NoteRequestEntity(updatedNote.title, updatedNote.content)

        given(noteService.updateNote(eq(user), argWhere { it.id == noteId && it.content == updatedNote.content })) willReturn { updatedNote }

        val response: ResponseEntity<Note> = notesController.updateNote(user, noteId, noteRequest)

        Assertions.assertEquals(HttpStatus.OK, response.statusCode)
        Assertions.assertEquals(updatedNote, response.body)
    }

    @Test
    fun `createNote should allow create note with empty content`() {
        val noteRequest = NoteRequestEntity("test title request", "")
        val note = Note(1, 1, noteRequest.title, noteRequest.content, Instant.now())

        given(noteService.createNote(user, noteRequest.title, noteRequest.content, null)) willReturn { note }

        val response = notesController.createNote(user, noteRequest)

        Assertions.assertEquals(HttpStatus.OK, response.statusCode)
        Assertions.assertEquals(note, response.body)
    }

    @Test
    fun `createNote should accept max length title and content`() {
        val maxTitle = "a".repeat(120)
        val maxContent = "a".repeat(255)
        val request = NoteRequestEntity(maxTitle, maxContent)
        val expectedNote = Note(id = 1, title = maxTitle, content = maxContent)

        given(noteService.createNote(user, maxTitle, maxContent, null)) willReturn { expectedNote }

        val response = notesController.createNote(user, request)
        Assertions.assertEquals(ResponseEntity.ok(expectedNote), response)
    }

    @Test
    fun `updateNote should accept max length title and content`() {
        val maxTitle = "a".repeat(120)
        val maxContent = "a".repeat(255)
        val request = NoteRequestEntity(maxTitle, maxContent)
        val expectedNote = Note(id = 1, title = maxTitle, content = maxContent)

        given(noteService.updateNote(any(), any())) willReturn { expectedNote }

        val response = notesController.updateNote(user, 1, request)
        Assertions.assertEquals(ResponseEntity.ok(expectedNote), response)
    }
}