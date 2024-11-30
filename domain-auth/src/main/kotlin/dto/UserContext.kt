package dto

import model.Role
import java.util.UUID

data class UserContext(
    val userId: UUID?,
    val role: Role?,
) {
    fun getIdIfRequired() = userId!!
    fun getRoleIfRequired() = role!!
}
