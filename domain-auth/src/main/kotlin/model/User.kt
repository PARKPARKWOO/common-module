package model

import java.time.LocalDateTime

data class User(
    val id: String,
    val email: String?,
    val password: String,
    val socialId: String?,
    val provider: String?,
    val role: Role,
    var createdAt: LocalDateTime,
    var updatedAt: LocalDateTime,
    var deletedAt: LocalDateTime?,
)
