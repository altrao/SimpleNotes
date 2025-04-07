package com.simplenotes.controller

import com.simplenotes.controller.model.NoteRequestEntity
import com.simplenotes.model.Note
import com.simplenotes.model.User
import com.simplenotes.service.NoteService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.time.temporal.ChronoUnit

@RestController
@RequestMapping("/api/notes")
class NotesController(
    private val noteService: NoteService
) {
    @PostMapping
    fun createNote(
        @AuthenticationPrincipal() user: User,
        @RequestBody note: NoteRequestEntity
    ): ResponseEntity<Note> {
        return ResponseEntity.ok(
            noteService.createNote(
                user,
                note.title,
                note.content,
                note.ttl?.let { Instant.now().plus(it, ChronoUnit.MINUTES) }
            )
        )
    }

    @GetMapping
    fun getNotes(
        @AuthenticationPrincipal user: User,
        @RequestParam(required = false) cursor: Instant?,
        @RequestParam(required = false) limit: Int?,
    ): ResponseEntity<List<Note>> {
        return ResponseEntity.ok(noteService.getNotes(user, cursor, limit))
    }

    @GetMapping("/deleted")
    fun getDeletedNotes(
        @AuthenticationPrincipal user: User,
        @RequestParam(required = false) cursor: Instant?,
        @RequestParam(required = false) limit: Int?,
    ): ResponseEntity<List<Note>> {
        return ResponseEntity.ok(noteService.getDeletedNotes(user, cursor, limit))
    }

    @GetMapping("/{noteId}")
    fun getNote(
        @AuthenticationPrincipal user: User,
        @PathVariable noteId: Long
    ): ResponseEntity<Note?> {
        val note = noteService.getNote(user, noteId)
        return if (note != null) {
            ResponseEntity.ok(note)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/{noteId}/versions")
    fun getVersions(
        @AuthenticationPrincipal user: User,
        @PathVariable noteId: Long,
        @RequestParam(required = false) cursor: Instant?,
        @RequestParam(required = false) limit: Int?
    ): ResponseEntity<List<Note>> {
        return ResponseEntity.ok(noteService.getVersions(user, noteId, cursor, limit))
    }

    @PutMapping("/{noteId}")
    fun updateNote(
        @AuthenticationPrincipal user: User,
        @PathVariable noteId: Long,
        @RequestBody note: NoteRequestEntity
    ): ResponseEntity<Note> {
        return ResponseEntity.ok(noteService.updateNote(user, Note(id = noteId, title = note.title, content = note.content)))
    }

    @DeleteMapping("/{noteId}")
    fun deleteNote(
        @AuthenticationPrincipal user: User,
        @PathVariable noteId: Long
    ): ResponseEntity<Unit> {
        noteService.deleteNote(user, noteId)
        return ResponseEntity.noContent().build()
    }
}