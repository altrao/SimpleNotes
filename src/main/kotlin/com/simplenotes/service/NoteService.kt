package com.simplenotes.service

import com.simplenotes.configuration.PaginationConfiguration
import com.simplenotes.exception.NoteException
import com.simplenotes.model.*
import com.simplenotes.repository.NoteRepository
import com.simplenotes.repository.SequenceRepository
import com.simplenotes.repository.UserNoteRepository
import com.simplenotes.validateTitleAndContentLength
import org.springframework.data.domain.Limit
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*


@Service
class NoteService(
    private val noteRepository: NoteRepository,
    private val sequenceRepository: SequenceRepository,
    private val userNoteRepository: UserNoteRepository,
    private val paginationConfiguration: PaginationConfiguration
) {
    private val initialVersion = 1L

    fun getNote(user: User, noteId: Long): Note? {
        if (user.id == null) {
            throw NoteException("Missing User ID")
        }

        return noteRepository.getNote(user.id, noteId)
    }

    fun getNotes(user: User, cursor: Instant? = null, limit: Int? = null): List<Note> {
        if (user.id == null) {
            throw NoteException("User ID is null")
        }

        return noteRepository.getActiveNotes(user.id, cursor ?: Instant.now(), getLimit(limit))
    }

    fun getDeletedNotes(user: User, cursor: Instant? = null, limit: Int? = null): List<Note> {
        if (user.id == null) {
            throw NoteException("User ID is null")
        }

        return noteRepository.getAllNotes(user.id, cursor ?: Instant.now(), getLimit(limit), true)
    }

    fun getAllNotes(user: User, cursor: Instant? = null, limit: Int? = null): List<Note> {
        if (user.id == null) {
            throw NoteException("User ID is null")
        }

        return noteRepository.getAllNotes(user.id, cursor ?: Instant.now(), getLimit(limit))
    }

    fun getVersions(user: User, noteId: Long, cursor: Instant? = null, limit: Int? = null): List<Note> {
        if (user.id == null) {
            throw NoteException("Missing User ID")
        }

        return noteRepository.getVersions(user.id, noteId, cursor ?: Instant.now(), getLimit(limit))
    }

    fun getVersion(user: User, noteId: Long, version: Int): Note? {
        if (user.id == null) {
            throw NoteException("Missing User ID")
        }

        return noteRepository.getByVersion(user.id, noteId, version)
    }

    fun createNote(user: User, title: String, content: String, expirationDate: Instant? = null): Note {
        if (user.id == null) {
            throw NoteException("Missing User ID")
        }


        validateTitleAndContentLength(title, content)

        if (expirationDate?.isBefore(Instant.now()) == true) {
            throw NoteException("Expiration date cannot be in the past")
        }

        return noteRepository.save(
            Note(
                id = sequenceRepository.next(),
                title = title,
                content = content,
                expirationDate = expirationDate,
                version = initialVersion
            )
        ).setActive(user.id)
    }

    fun updateNote(user: User, note: Note): Note {
        if (note.id == null || user.id == null) {
            throw NoteException("Missing Note or User ID")
        }

        validateTitleAndContentLength(note.title, note.content)

        val latestVersion = userNoteRepository.findById(UserNoteId(user.id, note.id)).takeIf { it.isPresent }?.get()
            ?: throw NoteException("Note not found")

        val latestNote = noteRepository.findById(NoteId(latestVersion.noteId, latestVersion.activeVersion)).takeIf { it.isPresent }?.get()
            ?: throw NoteException("Note not found")

        return noteRepository.save(latestNote.copy(title = note.title, content = note.content, version = latestVersion.activeVersion + 1)).setActive(user.id)
    }

    fun deleteNote(user: User, noteId: Long) {
        if (user.id == null) {
            throw NoteException("User ID is null")
        }

        val userNote = userNoteRepository.findById(UserNoteId(user.id, noteId)).takeIf { it.isPresent }?.get()
            ?: throw NoteException("Note not found")

        if (!userNote.deleted) {
            userNoteRepository.delete(noteId)
        }
    }

    private fun getLimit(limit: Int?): Limit {
        return Limit.of(minOf(limit ?: paginationConfiguration.limit, paginationConfiguration.limit))
    }

    private fun Note.setActive(userId: UUID): Note {
        userNoteRepository.save(UserNote(userId, id, version))
        return this
    }
}