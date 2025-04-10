package com.simplenotes.controller

import com.simplenotes.controller.model.ErrorResponseEntity
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
    fun handleAuthenticationExceptions(ex: RuntimeException): ResponseEntity<ErrorResponseEntity> {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponseEntity("Authentication failed"))
    }

    @ExceptionHandler(value = [RegisterException::class])
    fun handleRegisterExceptions(ex: RegisterException): ResponseEntity<ErrorResponseEntity> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponseEntity(ex.message))
    }

    @ExceptionHandler(value = [NoteException::class])
    fun handleNoteException(ex: NoteException): ResponseEntity<ErrorResponseEntity> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponseEntity(ex.message))
    }

    /**
     * Spring will throw this exception on an optimistic lock failure, for example if a specified version is different in the database.
     * May also be thrown if the entity is assumed to be present but is not.
     *
     * Read more: [org.springframework.dao.OptimisticLockingFailureException]
     *
     */
    @ExceptionHandler(value = [OptimisticLockingFailureException::class])
    fun handleOptimisticLockingException(ex: OptimisticLockingFailureException): ResponseEntity<ErrorResponseEntity> {
        logger.error("An error occurred while processing the request.", ex)

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponseEntity("An error occurred while processing the request."))
    }
}