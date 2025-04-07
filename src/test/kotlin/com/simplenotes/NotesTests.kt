package com.simplenotes

import com.simplenotes.exception.NoteException
import com.simplenotes.model.Note
import com.simplenotes.model.User
import com.simplenotes.model.UserNote
import com.simplenotes.repository.NoteRepository
import com.simplenotes.repository.UserNoteRepository
import com.simplenotes.repository.UserRepository
import com.simplenotes.service.NoteService
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.Limit
import org.springframework.test.context.TestPropertySource
import java.time.Instant
import java.time.ZoneId
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest(classes = [Application::class])
@TestPropertySource("classpath:application.yaml")
class NotesTests {
    @Autowired
    lateinit var userNoteRepository: UserNoteRepository

    @Autowired
    lateinit var noteRepository: NoteRepository

    @Autowired
    lateinit var noteService: NoteService

    @Autowired
    lateinit var userRepository: UserRepository

    @Test
    fun `should create Note`() {
        val user = userRepository.createUser("user-create-note", "test-password")
        val note = noteService.createNote(user, "test-note", "test-content")
        val persistedNote = noteService.getNote(user, note.id!!)

        assertNotNull(persistedNote)
        assertNotNull(note.creationDate)
        assertEquals(note.title, persistedNote.title)
        assertEquals(note.content, persistedNote.content)
        assertEquals(note.id, persistedNote.id)
        assertEquals(note.version, persistedNote.version)
    }

    @Test
    fun `should create Note with expiration date`() {
        val expirationDate = Instant.now().plusSeconds(60)
        val user = userRepository.createUser("user-create-note-expiration-date", "test-password")
        val note = noteService.createNote(user, "test-note",  "test-content", expirationDate)
        val persistedNote = noteService.getNote(user, note.id!!)

        assertNotNull(persistedNote)
        assertNotNull(persistedNote.expirationDate)

        val expirationDateTime = expirationDate.atZone(ZoneId.systemDefault())
        val persistedDateTime = persistedNote.expirationDate.atZone(ZoneId.systemDefault())

        assertEquals(expirationDateTime.year, persistedDateTime.year)
        assertEquals(expirationDateTime.month, persistedDateTime.month)
        assertEquals(expirationDateTime.dayOfMonth, persistedDateTime.dayOfMonth)
        assertEquals(expirationDateTime.hour, persistedDateTime.hour)
        assertEquals(expirationDateTime.minute, persistedDateTime.minute)
    }

    @Test
    fun `should throw NoteException if expiration date is in the past`() {
        val expirationDate = Instant.now().minusSeconds(60)
        val user = userRepository.createUser("user-create-note-expiration-date-past", "test-password")

        val exception = assertThrows<NoteException> {
            noteService.createNote(user, "test-note", "test-content",  expirationDate)
        }

        assertEquals("Expiration date cannot be in the past", exception.message)
    }

    @Test
    fun `should create new version on update`() {
        val user = userRepository.createUser("user-new-version-update", "test-password")
        val note = noteService.createNote(user, "test-note", "test-content")
        val persistedNote = noteService.getNote(user, note.id!!)

        assertNotNull(persistedNote)

        val updatedNote = noteService.updateNote(user, Note(id = persistedNote.id, title = "updated-title", content = "updated-content"))
        val updatedPersistedNote = noteService.getNote(user, updatedNote.id!!)

        assertNotNull(updatedPersistedNote)
        assertEquals(persistedNote.version + 1, updatedPersistedNote.version)
    }

    @Test
    fun `should keep all versions on update`() {
        val user = userRepository.createUser("user-all-versions-update", "test-password")
        val note = noteService.createNote(user, "test-note", "test-content")

        assertNotNull(note.id)

        repeat(4) {
            noteService.updateNote(user, Note(note.id, title = "updated-title", content = "updated-content"))
        }

        val allVersions = noteRepository.getVersions(user.id!!, note.id, Instant.now(), Limit.of(100))

        assertTrue { allVersions.isNotEmpty() }
        allVersions.map { it.version }.containsAll(listOf(1, 2, 3, 4, 5))
    }

    @Test
    fun `should return only active Notes`() {
        val user = userRepository.createUser("user-active-notes", "test-password")
        val note = noteService.createNote(user, "test-note", "test-content")
        val persistedNote = noteService.getNote(user, note.id!!)

        assertNotNull(persistedNote)

        repeat(4) {
            noteService.updateNote(user, Note(note.id, title = "updated-title-$it", content = "updated-content"))
        }

        val activeNotes = noteService.getNotes(user)
        assertTrue(activeNotes.isNotEmpty())
        assertTrue(activeNotes.size == 1)
    }

    @Test
    fun `should return specified version`() {
        val user = userRepository.createUser("user-specified-version", "test-password")
        val note = noteService.createNote(user, "test-note", "test-content")

        repeat(3) {
            noteService.updateNote(user, note.copy(note.id, title = "updated-title-${it + 2}"))
        }

        val noteV3 = noteService.getVersion(user, note.id!!, 3)
        assertNotNull(noteV3)
        assertEquals("updated-title-3", noteV3.title)
        assertEquals(3, noteV3.version)
    }

    @Test
    fun `should return all versions`() {
        val user = userRepository.createUser("user-return-all-versions", "test-password")

        val note1 = noteService.createNote(user, "test-note-1v1", "test-content-1")
        repeat(3) {
            noteService.updateNote(user, note1.copy(title = "test-note-1v${it + 2}"))
        }

        val note2 = noteService.createNote(user, "test-note-2v1", "test-content-2")
        repeat(5) {
            noteService.updateNote(user, note2.copy(title = "test-note-2v${it + 2}"))
        }

        val note3 = noteService.createNote(user, "test-note-3v1", "test-content-3")
        repeat(8) {
            noteService.updateNote(user, note3.copy(title = "test-note-3v${it + 2}"))
        }

        noteService.getVersions(user, note1.id!!).let { notes ->
            assertEquals(4, notes.size)
            notes.forEach {
                assertEquals("test-note-1v${it.version}", it.title)
            }
        }

        noteService.getVersions(user, note2.id!!).let { notes ->
            assertEquals(6, notes.size)
            notes.forEach {
                assertEquals("test-note-2v${it.version}", it.title)
            }
        }

        noteService.getVersions(user, note3.id!!).let { notes ->
            assertEquals(9, notes.size)
            notes.forEach {
                assertEquals("test-note-3v${it.version}", it.title)
            }
        }
    }

    @Test
    fun `should throw on trying update a different user's note`() {
        val user = userRepository.createUser("user-update-different-user-note", "test-password")
        val note = noteService.createNote(user, "test-note", "test-content")

        val otherUser = userRepository.createUser("user-update-different-user-note-1", "test-password")

        val exception = assertThrows<NoteException> {
            noteService.updateNote(otherUser, note)
        }

        assertEquals("Note not found", exception.message)
    }

    @Test
    fun `should throw NoteException when updating a non-existent note`() {
        val user = userRepository.createUser("user-update-non-existent-note", "test-password")
        val nonExistentNote = Note(id = 999L, title = "test-note", content = "test-content")

        val exception = assertThrows<NoteException> {
            noteService.updateNote(user, nonExistentNote)
        }

        assertEquals("Note not found", exception.message)
    }

    @Test
    fun `should delete`() {
        val user = userRepository.createUser("user-delete", "test-password")
        val note = noteService.createNote(user, "test-note", "test-content")

        noteService.deleteNote(user, note.id!!)
        assertTrue(noteService.getNotes(user).isEmpty())
    }

    @Test
    fun `should not delete for different user`() {
        val user = userRepository.createUser("user-not-delete-different-user-note", "test-password")
        val note = noteService.createNote(user, "test-note", "test-content")

        val otherUser = userRepository.createUser("test-user-1", "test-password")
        val exception = assertThrows<NoteException> {
            noteService.deleteNote(otherUser, note.id!!)
        }

        assertEquals("Note not found", exception.message)
    }

    @Test
    fun `should not return deleted`() {
        val user = userRepository.createUser("user-not-return-deleted", "test-password")
        val newNote = noteService.createNote(user, "test-note", "test-content")

        noteService.deleteNote(user, newNote.id!!)

        assertTrue(noteService.getNotes(user).isEmpty())
        assertTrue(noteService.getAllNotes(user).isEmpty())
    }

    @Test
    fun `should return all versions of a deleted note`() {
        val user = userRepository.createUser("user-get-all-versions-deleted-note", "test-password")
        val note = noteService.createNote(user, "test-note", "test-content")

        repeat(3) {
            noteService.updateNote(user, note.copy(title = "updated-title-${it + 2}"))
        }

        noteService.deleteNote(user, note.id!!)

        val allVersions = noteService.getVersions(user, note.id)

        assertEquals(4, allVersions.size)
        assertTrue { allVersions.map { it.version }.containsAll(listOf(1, 2, 3, 4)) }
    }

    @Test
    fun `should throw NoteException when deleting a non-existent note`() {
        val user = userRepository.createUser("user-delete-non-existent-note", "test-password")
        val nonExistentNoteId = 999L

        val exception = assertThrows<NoteException> {
            noteService.deleteNote(user, nonExistentNoteId)
        }

        assertEquals("Note not found", exception.message)
    }

    @Test
    fun `should restore deleted Note on update`() {
        val user = userRepository.createUser("user-restore-deleted-update", "test-password")
        val note = noteService.createNote(user, "test-note", "test-content")

        noteService.deleteNote(user, note.id!!)
        val activeNotes = noteService.getNotes(user)

        assertTrue(activeNotes.isEmpty())

        val restoredNote = noteService.updateNote(user, note.copy(title = "restored note"))
        assertEquals("restored note", restoredNote.title)

        assertNotNull(noteService.getNote(user, note.id))
    }

    @Test
    fun `should paginate active Notes`() {
        val user = userRepository.createUser("user-paginate-active-notes", "test-password")

        repeat(50) {
            noteService.createNote(user, "test-note-$it", "test-content-$it")
        }

        noteRepository.flush()

        val firstPage = noteService.getNotes(user, limit = 10)
        assertEquals(10, firstPage.size)

        val lastCursor = (1..4).fold(firstPage.last().creationDate!!) { acc, i ->
            noteService.getNotes(user, cursor = acc, limit = 10).also {
                assertEquals(10, it.size)
            }.last().creationDate!!
        }

        val lastPage = noteService.getNotes(user, cursor = lastCursor, limit = 10)

        assertEquals(0, lastPage.size)
        assertEquals(50, noteService.getNotes(user).size)
    }

    @Test
    fun `should paginate all versions`() {
        val user = userRepository.createUser(username = "user-paginate-all-versions", password = "test-password")
        val note = noteService.createNote(user, "test-note", "test-content")

        repeat(30) {
            noteService.updateNote(user, note)
        }

        noteRepository.flush()

        val lastCursor = (1..5).fold(Instant.now()) { acc, i ->
            noteService.getVersions(user, note.id!!, cursor = acc, limit = 6).last().creationDate!!
        }

        val lastPage = noteService.getVersions(user, note.id!!, cursor = lastCursor)

        // 30 new versions + 1 (original version) = 31
        assertEquals(1, lastPage.size)
        assertEquals(31, noteService.getVersions(user, note.id).size)
    }

    @Test
    fun `should paginate all Notes`() {
        val user = userRepository.createUser(username = "user-paginate-all-notes", password = "test-password")

        repeat(20) {
            noteService.createNote(user, "test-note-$it","test-content-$it")
        }

        noteService.getNotes(user).forEach { note ->
            repeat(4) {
                noteService.updateNote(user, note)
            }
        }

        val lastCursor = (1..4).fold(Instant.now()) { acc, i ->
            noteService.getAllNotes(user, cursor = acc, limit = 25).last().creationDate!!
        }

        val lastPage = noteService.getAllNotes(user, cursor = lastCursor)

        assertTrue(lastPage.isEmpty())
        assertEquals(100, noteService.getAllNotes(user).size)
    }

    @Test
    fun `should throw Exception on updating Note when Note or User id null`() {
        assertThrowsNoteException("Missing Note or User ID") { noteService.updateNote(User(id = UUID.randomUUID()), Note(id = null)) }
        assertThrowsNoteException("Missing Note or User ID") { noteService.updateNote(User(id = null), Note(id = 0)) }
        assertThrowsNoteException("Missing Note or User ID") { noteService.updateNote(User(id = null), Note(id = null)) }
    }

    @Test
    fun `should throw Exception on creating Note when User id is null`() {
        assertThrowsNoteException("Missing User ID") { noteService.createNote(User(id = null), "title", "content") }
    }

    @Test
    fun `should throw Exception on fetching all Notes when User id is null`() {
        assertThrowsNoteException("User ID is null") { noteService.getAllNotes(User(id = null)) }
    }

    @Test
    fun `should throw Exception on fetching active Notes when User id is null`() {
        assertThrowsNoteException("User ID is null") { noteService.getNotes(User(id = null)) }
    }

    @Test
    fun `should throw Exception on fetching a Note when User id is null`() {
        assertThrowsNoteException("Missing User ID") { noteService.getNote(User(id = null), 0) }
    }

    @Test
    fun `should throw Exception on fetching all Note versions when User id is null`() {
        assertThrowsNoteException("Missing User ID") { noteService.getVersions(User(id = null), 0) }
    }

    @Test
    fun `should throw Exception on deleting Note if User id is null`() {
        assertThrowsNoteException("User ID is null") { noteService.deleteNote(User(id = null), 0) }
    }

    @Test
    fun `should throw when updating note that exists in userNote but not in notes`() {
        val user = userRepository.createUser("user-update-missing-note", "test-password")
        val noteId = -999L

        userNoteRepository.save(UserNote(user.id!!, noteId, 1))

        val exception = assertThrows<NoteException> {
            noteService.updateNote(user, Note(id = noteId, title = "test", content = "test"))
        }

        assertEquals("Note not found", exception.message)
    }

    @Test
    fun `should throw when getting version with null User id`() {
        val exception = assertThrows<NoteException> {
            noteService.getVersion(User(id = null), 1, 1)
        }

        assertEquals("Missing User ID", exception.message)
    }

    @Test
    fun `should return deleted Notes`() {
        val user = userRepository.createUser("user-get-deleted-notes", "test-password")
        val note1 = noteService.createNote(user, "test-note-1", "test-content-1")
        val note2 = noteService.createNote(user, "test-note-2", "test-content-2")

        noteService.deleteNote(user, note1.id!!)
        noteService.deleteNote(user, note2.id!!)

        val deletedNotes = noteService.getDeletedNotes(user)
        assertEquals(2, deletedNotes.size)
        assertTrue(deletedNotes.all { it.id in listOf(note1.id, note2.id) })
    }

    @Test
    fun `should paginate deleted Notes`() {
        val user = userRepository.createUser("user-paginate-deleted-notes", "test-password")

        repeat(20) {
            val note = noteService.createNote(user, "test-note-$it", "test-content-$it")
            noteService.deleteNote(user, note.id!!)
        }

        val firstPage = noteService.getDeletedNotes(user, limit = 10)
        assertEquals(10, firstPage.size)

        val lastCursor = (1..1).fold(firstPage.last().creationDate!!) { acc, i ->
            noteService.getDeletedNotes(user, cursor = acc, limit = 10).also {
                assertEquals(10, it.size)
            }.last().creationDate!!
        }

        val lastPage = noteService.getDeletedNotes(user, cursor = lastCursor)
        assertEquals(0, lastPage.size)
    }

    @Test
    fun `should throw Exception when getting deleted Notes with null User id`() {
        assertThrowsNoteException("User ID is null") {
            noteService.getDeletedNotes(User(id = null))
        }
    }

    private fun assertThrowsNoteException(message: String, block: () -> Unit) {
        val exception = assertThrows<NoteException> { block() }
        assertEquals(message, exception.message)
    }
}
