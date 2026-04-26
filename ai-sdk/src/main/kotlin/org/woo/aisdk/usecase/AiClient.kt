package org.woo.aisdk.usecase

import io.grpc.ClientInterceptor
import org.woo.ai.grpc.AiProto

interface AiClient {
    suspend fun chat(
        messages: List<Pair<String, String>>,
        applicationId: String,
        sessionId: String,
        models: List<Pair<AiProto.Vendor, String>> = emptyList(),
        timeoutSeconds: Int? = null,
        responseSchema: String? = null,
        maxTokens: Int? = null,
        requestType: String? = null,
        vararg interceptors: ClientInterceptor,
    ): AiProto.AiApiResponse

    suspend fun embedding(
        texts: List<String>,
        applicationId: String,
        models: List<Pair<AiProto.Vendor, String>> = emptyList(),
        vararg interceptors: ClientInterceptor,
    ): AiProto.EmbeddingApiResponse

    /**
     * 멀티모달 Vision 호출. 텍스트 프롬프트와 이미지 리스트를 함께 전달한다.
     *
     * 이미지 소스는 [VisionImage] sealed 클래스의 3가지 팩터리를 이용:
     *  - [VisionImage.base64]
     *  - [VisionImage.url]        (권장: Storage presigned URL)
     *  - [VisionImage.storageKey]
     *
     * spring-ai 서버는 URL 호스트 화이트리스트(`vision.allowed-hosts`)로
     * SSRF 를 방지하므로, 허용 목록 외 URL 은 400 처리된다.
     * 현재 Google(Gemini) 벤더만 구현되어 있다.
     */
    suspend fun vision(
        messages: List<Pair<String, String>>,
        images: List<VisionImage>,
        applicationId: String,
        models: List<Pair<AiProto.Vendor, String>> = emptyList(),
        timeoutSeconds: Int? = null,
        maxTokens: Int? = null,
        requestType: String? = null,
        vararg interceptors: ClientInterceptor,
    ): AiProto.AiVisionResponse

    /**
     * 멀티모달 Document(PDF) 호출. 텍스트 프롬프트와 PDF URL 리스트를 함께 전달한다.
     *
     * 현재 [DocumentSource.Url] 만 지원 (Storage presigned URL 또는 공개 URL).
     * spring-ai 서버는 URL 호스트 화이트리스트(`vision.allowed-hosts`)로 SSRF 를 방지하고,
     * Gemini inlineData 한도(20MB) 초과 PDF 는 거부한다.
     * 현재 Google(Gemini) 벤더만 구현되어 있다.
     */
    suspend fun document(
        messages: List<Pair<String, String>>,
        documents: List<DocumentSource>,
        applicationId: String,
        sessionId: String,
        models: List<Pair<AiProto.Vendor, String>> = emptyList(),
        timeoutSeconds: Int? = null,
        maxTokens: Int? = null,
        requestType: String? = null,
        vararg interceptors: ClientInterceptor,
    ): AiProto.AiDocumentResponse
}
