package com.simplenotes.service

import com.simplenotes.model.Note
import com.simplenotes.repository.NoteRepository
import com.simplenotes.repository.UserNoteRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.then
import org.mockito.kotlin.willReturn
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

class ExpirationSchedulerTest {
    private lateinit var noteRepository: NoteRepository
    private lateinit var userNoteRepository: UserNoteRepository
    private lateinit var expirationScheduler: ExpirationScheduler

    @BeforeEach
    fun setUp() {
        noteRepository = Mockito.mock(NoteRepository::class.java)
        userNoteRepository = Mockito.mock(UserNoteRepository::class.java)
        expirationScheduler = ExpirationScheduler(noteRepository, userNoteRepository)
    }

    @Test
    fun `cleanExpiredNotes should delete expired notes in batches`() {
        val pageable = PageRequest.of(0, 100)

        val note1 = Mockito.mock(Note::class.java).also { given(it.id).willReturn(1L) }
        val note2 = Mockito.mock(Note::class.java).also { given(it.id).willReturn(2L) }
        val note3 = Mockito.mock(Note::class.java).also { given(it.id).willReturn(3L) }
        val note4 = Mockito.mock(Note::class.java).also { given(it.id).willReturn(4L) }

        val firstPage: Page<Note> = PageImpl(listOf(note1, note2), pageable, 2)
        val secondPage: Page<Note> = PageImpl(listOf(note3, note4), pageable, 2)
        val emptyPage: Page<Note> = PageImpl(emptyList(), pageable, 0)

        given(noteRepository.getExpiredNotes(any(), any())).willReturn(firstPage, secondPage, emptyPage)

        expirationScheduler.cleanExpiredNotes()

        Mockito.verify(noteRepository, Mockito.times(3)).getExpiredNotes(any(), any())
        Mockito.verify(userNoteRepository, Mockito.times(1)).delete(note1.id!!)
        Mockito.verify(userNoteRepository, Mockito.times(1)).delete(note2.id!!)
        Mockito.verify(userNoteRepository, Mockito.times(1)).delete(note3.id!!)
        Mockito.verify(userNoteRepository, Mockito.times(1)).delete(note4.id!!)
    }

    @Test
    fun `cleanExpiredNotes should handle empty pages`() {
        val pageable = PageRequest.of(0, 100)
        val emptyPage: Page<Note> = PageImpl(emptyList(), pageable, 0)

        given(noteRepository.getExpiredNotes(any(), any())) willReturn { emptyPage }

        expirationScheduler.cleanExpiredNotes()

        then(noteRepository).should(Mockito.times(1)).getExpiredNotes(any(), any())
        then(userNoteRepository).should(Mockito.never()).delete(any<Long>())
    }
}