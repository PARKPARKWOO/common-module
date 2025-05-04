package dto

import exception.UserContextNotLoadedException
import model.Role
import java.util.UUID

// all application level
data class Passport(
    val userId: UUID,
    val role: Role,
    val signInApplicationId: String,
    private var userContext: UserContext?,
) {
    /**
     * userContext가 비어 있을 때만 fetcher를 호출해 채워주고,
     * 항상 non-null한 UserContext를 반환.
     */
    suspend fun ensureUserContextLoaded(fetcher: suspend () -> UserContext): UserContext =
        userContext ?: fetcher().also { userContext = it }

    /**
     * 호출한 타이밍에 userContext가 이미 있으면 즉시 반환,
     * 없으면 null을 반환.
     */
    fun getUserContextIfPresent(): UserContext? = userContext

    /**
     * 호출 시 무조건 userContext가 있어야 한다면 사용.
     * 없으면 IllegalStateException을 던짐.
     */
    fun requireUserContext(): UserContext = this.userContext ?: throw UserContextNotLoadedException()
}

// specific application level
data class UserContext(
    val email: String?,
    val userName: String?,
    val applicationRole: String,
    val accessLevel: Int,
) {
    fun getNameIfRequired() = userName!!

    fun getEmailIfRequired() = email!!
}
