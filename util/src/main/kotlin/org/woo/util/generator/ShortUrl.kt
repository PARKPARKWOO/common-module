package org.woo.util.generator

import java.security.MessageDigest
import java.util.Random

object ShortUrl {
    private const val ELEMENTS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    private val random = Random()

    /**
     * 주어진 값이 없을때는 랜덤으로 생성한다.
     */
    fun generateByBase62(length: Int): String {
        val sb = StringBuilder()
        for (i in 0 until length) {
            val idx = random.nextInt(ELEMENTS.length)
            sb.append(ELEMENTS[idx])
        }
        return sb.toString()
    }

    fun generateByMD5(
        origin: String,
        length: Int,
    ): String {
        val md5 = MessageDigest.getInstance("MD5")
        val digest = md5.digest(origin.toByteArray())
        val hexString = digest.joinToString("") { "%02x".format(it) }
        return hexString.take(length)
    }
}
