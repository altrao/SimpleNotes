package com.simplenotes.model

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.io.Serializable
import java.util.*

@Entity
@IdClass(UserNoteId::class)
@Table(indexes = [
    Index(name = "idx_usernote_version", columnList = "activeVersion"),
    Index(name = "idx_usernote_deleted", columnList = "deleted")
])
data class UserNote(
    @Id
    val userId: UUID? = null,
    @Id
    val noteId: Long? = null,
    val activeVersion: Long = 1,
    var deleted: Boolean = false
)

data class UserNoteId(
    val userId: UUID? = null,
    val noteId: Long? = null
) : Serializable