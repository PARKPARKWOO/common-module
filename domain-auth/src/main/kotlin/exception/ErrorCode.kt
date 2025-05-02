package exception

enum class ErrorCode(
    val message: String,
    val httpCode: Int,
    val level: LogLevel,
) {
    NO_BEARER_TOKEN("", 401, LogLevel.WARN),
    EXPIRED_JWT("", 401, LogLevel.WARN),
    PARSE_JWT_FAILED("", 401, LogLevel.WARN),
    REISSUE_JWT_TOKEN_FAILURE("", 401, LogLevel.WARN),

    FORBIDDEN("작업을 수행할 권한이 없습니다.", 403, LogLevel.WARN),
}
