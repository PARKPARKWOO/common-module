package dto

import model.Role
import java.util.UUID

data class UserContext(
    val userId: UUID?,
    val role: Role?,
    val userName: String?,
    val email: String?,
    val signInApplicationId: String,
) {
    fun getIdIfRequired() = userId!!

    fun getRoleIfRequired() = role!!

    fun getNameIfRequired() = userName!!

    fun getEmailIfRequired() = email!!
}
