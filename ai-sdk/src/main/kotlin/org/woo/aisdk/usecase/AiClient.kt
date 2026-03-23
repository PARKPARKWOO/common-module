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
}
