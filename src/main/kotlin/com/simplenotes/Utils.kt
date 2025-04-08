package com.simplenotes

import com.simplenotes.exception.NoteException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

inline fun <reified T> T.logger(): Logger {
    return LoggerFactory.getLogger(T::class.java)
}

fun validateTitleAndContentLength(title: String, content: String) {
    if (title.length > 120) {
        throw NoteException("Note title cannot exceed 120 characters")
    }
    if (content.length > 255) {
        throw NoteException("Note content cannot exceed 255 characters")
    }
}
