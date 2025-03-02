package org.woo.util.generator

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ShortUrlTest {
    companion object {
        private const val ELEMENTS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    }

    fun testGenerateByBase62RandomVersion() {
        val length = 7
        val shortUrl = ShortUrl.generateByBase62(length)
        // 실제 길이: length + 1
        assertEquals(length + 1, shortUrl.length, "Returned string length should be ${length + 1}")

        // ELEMENTS 문자열에 포함된 문자만 있는지 확인
        for (char in shortUrl) {
            assertTrue(
                ELEMENTS.contains(char),
                "Character '$char' should be in the allowed ELEMENTS",
            )
        }
    }

    @Test
    fun `test generateByBase62 with origin`() {
        val length = 7
        val origin = "example"
        val shortUrl = ShortUrl.generateByBase62(length, origin)
        // 현재 구현은 빈 문자열을 리턴하므로 빈 문자열이 반환되는지 확인
        assertEquals("", shortUrl, "For now, generateByBase62(origin) should return an empty string")
    }

    @Test
    fun `test generateByMD5`() {
        val origin = "test"
        val desiredLength = 7
        val shortUrl = ShortUrl.generateByMD5(origin, desiredLength)
        // 반환된 문자열 길이가 원하는 길이와 일치하는지 확인
        assertEquals(desiredLength, shortUrl.length, "MD5 hash substring should be $desiredLength characters long")

        // MD5의 결과는 16진수 문자열이므로, 0-9, a-f만 포함해야 함
        val hexPattern = Regex("^[0-9a-f]+$")
        assertTrue(hexPattern.matches(shortUrl), "MD5 hash substring should be in hexadecimal format")
    }
}
