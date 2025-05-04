package exception

open class AuthException(
    open val errorCode: ErrorCode,
    override val cause: Throwable?,
) : RuntimeException(errorCode.message, cause)

data class ExpiredJwtException(
    override val errorCode: ErrorCode,
    override val cause: Throwable?,
) : AuthException(errorCode = errorCode, cause = cause)

data class ParseJwtFailedException(
    override val errorCode: ErrorCode,
    override val cause: Throwable?,
) : AuthException(errorCode = errorCode, cause = cause)

data class NoBearerTokenException(
    override val errorCode: ErrorCode,
    override val cause: Throwable?,
) : AuthException(errorCode = errorCode, cause = cause)

data class MalFormedTokenException(
    override val errorCode: ErrorCode,
    override val cause: Throwable?,
) : AuthException(errorCode = errorCode, cause = cause)

data class UserContextNotLoadedException(
    override val errorCode: ErrorCode = ErrorCode.USER_CONTEXT_NOT_LOADED,
    override val cause: Throwable? = null,
) : AuthException(errorCode, cause)
