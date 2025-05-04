package dto

import model.Role
import java.util.UUID

// all application level
data class Passport(
    val userId: UUID,
    val role: Role,
    val signInApplicationId: String,
    val userContext: UserContext?,
)

// specific application level
data class UserContext(
    val email: String?,
    val userName: String?,
    val role: String,
    val applicationRole: String,
    val accessLevel: Int,
) {
    fun getNameIfRequired() = userName!!

    fun getEmailIfRequired() = email!!
}
