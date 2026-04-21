package org.woo.aisdk.usecase

import com.google.protobuf.ByteString
import org.woo.ai.grpc.AiProto

/**
 * Vision 호출 시 전달하는 이미지 소스. proto [AiProto.ImageInput] 를
 * 직접 빌드하기 부담스러워 타입 안전한 sealed 계층을 제공한다.
 */
sealed class VisionImage {

    /** 내부에서 proto 변환용. AiService 에서만 호출된다. */
    internal abstract fun toProto(): AiProto.ImageInput

    data class Base64(val data: ByteArray, val mimeType: String) : VisionImage() {
        override fun toProto(): AiProto.ImageInput =
            AiProto.ImageInput.newBuilder()
                .setBytes(
                    AiProto.BytesImage.newBuilder()
                        .setData(ByteString.copyFrom(data))
                        .setMimeType(mimeType)
                        .build(),
                )
                .build()

        // ByteArray 는 자동 equals/hashCode 가 contentEquals 가 아니므로 재정의
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Base64) return false
            return mimeType == other.mimeType && data.contentEquals(other.data)
        }

        override fun hashCode(): Int = 31 * data.contentHashCode() + mimeType.hashCode()
    }

    data class Url(val url: String) : VisionImage() {
        override fun toProto(): AiProto.ImageInput =
            AiProto.ImageInput.newBuilder()
                .setUrl(AiProto.UrlImage.newBuilder().setUrl(url).build())
                .build()
    }

    data class StorageKey(val bucket: String, val key: String) : VisionImage() {
        override fun toProto(): AiProto.ImageInput =
            AiProto.ImageInput.newBuilder()
                .setStorageKey(
                    AiProto.StorageKeyImage.newBuilder()
                        .setBucket(bucket)
                        .setKey(key)
                        .build(),
                )
                .build()
    }

    companion object {
        fun base64(data: ByteArray, mimeType: String): VisionImage = Base64(data, mimeType)
        fun url(url: String): VisionImage = Url(url)
        fun storageKey(bucket: String, key: String): VisionImage = StorageKey(bucket, key)
    }
}
