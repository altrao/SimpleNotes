package com.simplenotes.model

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.io.Serializable
import java.time.Instant

@Entity
@IdClass(NoteId::class)
@Table(indexes = [
    Index(name = "idx_note_creation_date", columnList = "creationDate"),
    Index(name = "idx_note_expiration_date", columnList = "expirationDate")
])
data class Note(
    @Id
    val id: Long? = null,
    @Id
    val version: Long = 1,
    val title: String = "",
    val content: String = "",
    @CreationTimestamp
    val creationDate: Instant? = null,
    val expirationDate: Instant? = null
)

data class NoteId(
    val id: Long? = null,
    val version: Long? = null
) : Serializable