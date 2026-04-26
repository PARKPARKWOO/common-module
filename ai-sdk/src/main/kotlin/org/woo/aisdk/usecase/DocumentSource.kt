package org.woo.aisdk.usecase

import org.woo.ai.grpc.AiProto

/**
 * Document(PDF) 호출 시 전달하는 소스. proto [AiProto.DocumentInput] 를
 * 직접 빌드하기 부담스러워 타입 안전한 sealed 계층을 제공한다.
 *
 * 현재 [Url] 만 지원. 추후 Bytes / StorageKey 추가 가능.
 */
sealed class DocumentSource {

    /** 내부에서 proto 변환용. AiService 에서만 호출된다. */
    internal abstract fun toProto(): AiProto.DocumentInput

    data class Url(val url: String) : DocumentSource() {
        override fun toProto(): AiProto.DocumentInput =
            AiProto.DocumentInput.newBuilder()
                .setUrl(AiProto.PdfUrlInput.newBuilder().setUrl(url).build())
                .build()
    }

    companion object {
        fun url(url: String): DocumentSource = Url(url)
    }
}
