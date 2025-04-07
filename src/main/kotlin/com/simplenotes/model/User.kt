package com.simplenotes.model

import jakarta.persistence.*
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import java.util.*

@Entity(name = "NotesUser")
@Table(indexes = [
    Index(name = "idx_user_username", columnList = "username")
])
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    @Column(name = "username", unique = true)
    val email: String = "defaultUser",
    @Column(name = "password")
    val pass: String = "defaultPassword",
    @Enumerated(EnumType.STRING)
    val role: Role = Role.USER
) : User(email, pass, getAuthorities(role)) {
    companion object {
        fun getAuthorities(role: Role): List<GrantedAuthority> {
            return listOf(SimpleGrantedAuthority("ROLE_${role.name}"))
        }
    }
}