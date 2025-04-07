package com.simplenotes.repository

import com.simplenotes.model.Role
import com.simplenotes.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
@JvmDefaultWithCompatibility
interface UserRepository : JpaRepository<User, UUID> {
    fun createUser(username: String, password: String): User {
        return save(User(email = username, pass = password, role = Role.USER))
    }

    fun findByEmail(email: String): User?
}
