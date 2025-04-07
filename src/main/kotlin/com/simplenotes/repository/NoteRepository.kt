package com.simplenotes.repository

import com.simplenotes.model.Note
import com.simplenotes.model.NoteId
import org.springframework.data.domain.Limit
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*


@Repository
interface NoteRepository : JpaRepository<Note, NoteId> {
    @Query("SELECT n FROM Note n JOIN UserNote un ON un.noteId = n.id AND un.activeVersion = n.version WHERE n.id = :id AND un.userId = :user")
    fun getNote(user: UUID, id: Long): Note?

    @Query("SELECT n FROM Note n JOIN UserNote un ON un.noteId = n.id AND un.activeVersion = n.version WHERE un.userId = :user AND un.deleted = false AND n.creationDate < :cursor ORDER BY n.creationDate DESC")
    fun getActiveNotes(user: UUID, cursor: Instant, limit: Limit): List<Note>

    @Query("SELECT n FROM Note n JOIN UserNote un ON un.noteId = n.id AND un.userId = :userId AND un.deleted = :deleted AND n.creationDate < :cursor ORDER BY n.creationDate DESC")
    fun getAllNotes(userId: UUID, cursor: Instant, limit: Limit, deleted: Boolean = false): List<Note>

    @Query("SELECT n FROM Note n JOIN UserNote un ON un.noteId = n.id WHERE un.userId = :userId AND n.id = :id AND n.creationDate < :cursor ORDER BY n.creationDate DESC")
    fun getVersions(userId: UUID, id: Long, cursor: Instant, limit: Limit): List<Note>

    @Query("SELECT n FROM Note n JOIN UserNote un ON un.noteId = n.id WHERE un.userId = :userId AND n.version = :version AND n.id = :id ORDER BY n.creationDate DESC")
    fun getByVersion(userId: UUID, id: Long, version: Int): Note?

    @Query("SELECT n FROM Note n JOIN UserNote un ON un.noteId = n.id WHERE un.deleted = false AND n.expirationDate < :now")
    fun getExpiredNotes(now: Instant, pageable: Pageable): Page<Note>
}
