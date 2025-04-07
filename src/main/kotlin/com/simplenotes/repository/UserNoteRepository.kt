package com.simplenotes.repository

import com.simplenotes.model.UserNote
import com.simplenotes.model.UserNoteId
import jakarta.transaction.Transactional
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface UserNoteRepository : JpaRepository<UserNote, UserNoteId> {
    @Transactional
    @Modifying
    @Query("UPDATE UserNote SET deleted = true WHERE noteId = :noteId")
    fun delete(noteId: Long)
}