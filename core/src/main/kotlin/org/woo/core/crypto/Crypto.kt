package org.woo.core.crypto

import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class Crypto(
    private val base64AesKey: ByteArray,
    private val base64HmacKey: ByteArray,
) {
    class Envelope(
        val iv: ByteArray,
        val ciphertext: ByteArray,
    )

    private lateinit var aesSecretKey: SecretKey
    private lateinit var hmacSecretKey: SecretKey
    private val rnd: SecureRandom = SecureRandom.getInstanceStrong()

    init {
        val decodeHmacKey = Base64.getDecoder().decode(base64HmacKey)
        val decodeAesKey = Base64.getDecoder().decode(base64AesKey)
        require(decodeAesKey.size in setOf(16, 24, 32)) { "AES key must be 16/24/32 bytes, was ${decodeAesKey.size}" }
        require(decodeAesKey.size >= 32) { "HMAC-SHA256 key must be >= 32 bytes, was ${decodeHmacKey.size}" }
        aesSecretKey = SecretKeySpec(decodeAesKey, "AES")
        hmacSecretKey = SecretKeySpec(decodeHmacKey, "HmacSHA256")
    }

    fun encrypt(origin: String): Envelope {
        val iv: ByteArray = ByteArray(12)
        rnd.nextBytes(iv)
        val cipher: Cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, aesSecretKey, GCMParameterSpec(128, iv))
        val ct: ByteArray = cipher.doFinal(origin.toByteArray(StandardCharsets.UTF_8))
        return Envelope(iv, ct)
    }

    fun decrypt(envelope: Envelope): String {
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, aesSecretKey, GCMParameterSpec(128, envelope.iv))
        val pt = c.doFinal(envelope.ciphertext)
        return String(pt, StandardCharsets.UTF_8)
    }
}
