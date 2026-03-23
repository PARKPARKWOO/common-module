package org.woo.aisdk.usecase

import io.grpc.ClientInterceptor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.woo.ai.grpc.AiApiServiceGrpcKt
import org.woo.ai.grpc.AiProto
import org.woo.grpc.circuitbreaker.GrpcCircuitBreaker
import org.woo.grpc.circuitbreaker.ServiceStateRegistry

class AiService(
    private val stub: AiApiServiceGrpcKt.AiApiServiceCoroutineStub,
    private val circuitBreaker: GrpcCircuitBreaker,
    private val dispatcher: CoroutineDispatcher,
) : AiClient {
    companion object {
        private const val CHAT_METHOD_NAME = "Chat"
        private const val EMBEDDING_METHOD_NAME = "Embedding"
    }

    init {
        ServiceStateRegistry.initGrpcService(AiApiServiceGrpcKt.serviceDescriptor)
    }

    override suspend fun chat(
        messages: List<Pair<String, String>>,
        applicationId: String,
        sessionId: String,
        models: List<Pair<AiProto.Vendor, String>>,
        timeoutSeconds: Int?,
        responseSchema: String?,
        maxTokens: Int?,
        requestType: String?,
        vararg interceptors: ClientInterceptor,
    ): AiProto.AiApiResponse {
        val requestBuilder =
            AiProto.AiApiRequest
                .newBuilder()
                .setApplicationId(applicationId)
                .setSessionId(sessionId)
                .addAllMessages(
                    messages.map { (role, content) ->
                        AiProto.ChatMessage
                            .newBuilder()
                            .setRole(role)
                            .setContent(content)
                            .build()
                    },
                )
        if (models.isNotEmpty()) {
            requestBuilder.addAllModels(
                models.map { (vendor, version) ->
                    AiProto.ModelSpec
                        .newBuilder()
                        .setVendor(vendor)
                        .setVersion(version)
                        .build()
                },
            )
        }
        if (responseSchema != null) requestBuilder.setResponseSchema(responseSchema)
        if (timeoutSeconds != null) requestBuilder.setTimeoutSeconds(timeoutSeconds)
        if (maxTokens != null) requestBuilder.setMaxTokens(maxTokens)
        if (requestType != null) requestBuilder.setRequestType(requestType)

        circuitBreaker.checkCircuitBreaker(CHAT_METHOD_NAME)

        return withContext(dispatcher) {
            val stubWithInterceptors =
                if (interceptors.isNotEmpty()) {
                    stub.withInterceptors(*interceptors)
                } else {
                    stub
                }
            stubWithInterceptors.chat(requestBuilder.build())
        }
    }

    override suspend fun embedding(
        texts: List<String>,
        applicationId: String,
        models: List<Pair<AiProto.Vendor, String>>,
        vararg interceptors: ClientInterceptor,
    ): AiProto.EmbeddingApiResponse {
        val requestBuilder =
            AiProto.EmbeddingApiRequest
                .newBuilder()
                .setApplicationId(applicationId)
                .addAllTexts(texts)
        if (models.isNotEmpty()) {
            requestBuilder.addAllModels(
                models.map { (vendor, version) ->
                    AiProto.EmbeddingModelSpec
                        .newBuilder()
                        .setVendor(vendor)
                        .setVersion(version)
                        .build()
                },
            )
        }

        circuitBreaker.checkCircuitBreaker(EMBEDDING_METHOD_NAME)

        return withContext(dispatcher) {
            val stubWithInterceptors =
                if (interceptors.isNotEmpty()) {
                    stub.withInterceptors(*interceptors)
                } else {
                    stub
                }
            stubWithInterceptors.embedding(requestBuilder.build())
        }
    }
}
