package org.woo.mapper

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.woo.mapper.serializer.BigDecimalToStringSerializer
import java.math.BigDecimal

object Jackson {
    private const val EMPTY_STRING = ""

    private val EMPTY_BYTE_ARRAY = ByteArray(0)

    private val MAPPER = jacksonObjectMapper()
        // REGISTER MODULE
        .registerModules(
            JavaTimeModule(),
            SimpleModule()
                .addSerializer(BigDecimal::class.java, BigDecimalToStringSerializer()),
        )
        // SET SERIALIZATION INCLUSION
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        // CONFIGURE: SERIALIZATION_FEATURE
        .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true)
        // CONFIGURE: DESERIALIZATION_FEATURE
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    fun mapper(): ObjectMapper {
        return MAPPER
    }

    fun writeValueAsString(message: Any): String {
        return try {
            MAPPER.writeValueAsString(message)
        } catch (e: JsonProcessingException) {
            EMPTY_STRING
        }
    }

    fun writeValueAsBytes(message: Any): ByteArray {
        return try {
            MAPPER.writeValueAsBytes(message)
        } catch (e: JsonProcessingException) {
            EMPTY_BYTE_ARRAY
        }
    }

    fun <T> readValue(message: String, valueType: Class<T>): T? {
        return try {
            return MAPPER.readValue(message, valueType)
        } catch (e: JsonProcessingException) {
            null
        }
    }
}
