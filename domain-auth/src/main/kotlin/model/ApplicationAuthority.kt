package model

data class ApplicationAuthority(
    val id: Long,
    val applicationId: String,
    val authority: String,
    var level: Int,
)
