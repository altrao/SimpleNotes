package com.simplenotes

import com.simplenotes.model.UserNote
import com.simplenotes.model.UserNoteId
import com.simplenotes.repository.UserNoteRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.*

@SpringBootTest
open class UserNoteRepositoryTest {

    @Autowired
    private lateinit var userNoteRepository: UserNoteRepository

    @Test
    fun `delete should set deleted to true for given note`() {
        val userId = UUID.randomUUID()
        val noteId = 3L
        val userNote = UserNote(userId, noteId, activeVersion = 1, deleted = false)

        userNoteRepository.saveAndFlush(userNote)
        userNoteRepository.delete(noteId)

        val updatedUserNote = userNoteRepository.findById(UserNoteId(userId, noteId)).get()
        assertEquals(true, updatedUserNote.deleted)
    }

    @Test
    fun `delete should not affect other notes`() {
        val userId = UUID.randomUUID()
        val noteId1 = 1L
        val noteId2 = 2L
        val userNote1 = UserNote(userId, noteId1, activeVersion = 1, deleted = false)
        val userNote2 = UserNote(userId, noteId2, activeVersion = 2, deleted = false)

        userNoteRepository.save(userNote1)
        userNoteRepository.saveAndFlush(userNote2)
        userNoteRepository.delete(noteId1)

        val updatedUserNote1 = userNoteRepository.findById(UserNoteId(userId, noteId1)).get()
        val updatedUserNote2 = userNoteRepository.findById(UserNoteId(userId, noteId2)).get()
        assertEquals(true, updatedUserNote1.deleted)
        assertEquals(false, updatedUserNote2.deleted)
    }
}