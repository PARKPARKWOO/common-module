package org.woo.storagesdk.interceptor

enum class AllowExtension(
    private val extensions: List<String>,
) {
    VIDEO(listOf(".mp4", ".mkv", ".avi", ".mov", ".webm", ".flv", ".wmv", ".m4v", ".3gp")),
    IMAGE(listOf(".jpg", ".jpeg", ".png", "gif")),
    FILE(listOf(".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx", ".txt", ".owpml", ".zip")),
    AUDIO(listOf(".mp3", ".wav", ".aac", ".ogg", ".wave")), ;

    fun matches(ext: String) = extensions.any { it.equals(ext, ignoreCase = true) }
}
