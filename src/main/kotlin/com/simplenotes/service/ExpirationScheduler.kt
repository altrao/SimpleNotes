package com.simplenotes.service

import com.simplenotes.logger
import com.simplenotes.repository.NoteRepository
import com.simplenotes.repository.UserNoteRepository
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant


@Component
class ExpirationScheduler(
    private val noteRepository: NoteRepository,
    private val userNoteRepository: UserNoteRepository
) {
    private val logger = logger()

        @Scheduled(fixedRate = 60_000)
//    @Scheduled(fixedRate = 5_000)
    fun cleanExpiredNotes() {
        logger.debug("Cleaning expired notes...")

        val pageable = PageRequest.of(0, 100)
        val expiringTime = Instant.now()

        var page = noteRepository.getExpiredNotes(expiringTime, pageable)
        var total = 0
        while (page.hasContent()) {
            total = page.size
            page.content.forEach { userNoteRepository.delete(it.id!!) }
            page = noteRepository.getExpiredNotes(expiringTime, page.nextPageable())
        }

        logger.debug("Deleted $total expired notes")
    }
}
