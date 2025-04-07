package com.simplenotes.repository

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Component

@JvmDefaultWithCompatibility
interface SequenceRepository {
    fun next(): Long
}

@Component
class Sequence : SequenceRepository {
    @PersistenceContext
    private lateinit var entityManager: EntityManager

    override fun next(): Long {
        return entityManager.createQuery("select nextval('note_seq')").singleResult as Long
    }
}