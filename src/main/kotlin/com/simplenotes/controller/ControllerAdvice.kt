package com.simplenotes.controller

import com.simplenotes.exception.NoteException
import com.simplenotes.exception.RegisterException
import com.simplenotes.logger
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.security.SignatureException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class ControllerAdvice {
    private val logger = logger()

    @ExceptionHandler(value = [JwtException::class, AuthenticationException::class, SignatureException::class])
    fun handleAuthenticationExceptions(ex: RuntimeException): ResponseEntity<String> {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication failed.")
    }

    @ExceptionHandler(value = [RegisterException::class])
    fun handleRegisterExceptions(ex: RegisterException): ResponseEntity<String> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.message)
    }

    @ExceptionHandler(value = [NoteException::class])
    fun handleNoteException(ex: NoteException): ResponseEntity<String> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.message)
    }

    /**
     * Spring will throw this exception on an optimistic lock failure, for example if a specified version is different in the database.
     * May also be thrown if the entity is assumed to be present but is not.
     *
     * Read more: [org.springframework.dao.OptimisticLockingFailureException]
     *
     */
    @ExceptionHandler(value = [OptimisticLockingFailureException::class])
    fun handleOptimisticLockingException(ex: OptimisticLockingFailureException): ResponseEntity<String> {
        logger.error("An error occurred while processing the request.", ex)

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred.")
    }
}