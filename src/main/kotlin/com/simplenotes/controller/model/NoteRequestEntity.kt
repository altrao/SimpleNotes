package com.simplenotes.controller.model

data class NoteRequestEntity(
    val title: String,
    val content: String,
    val ttl: Long? = null
)