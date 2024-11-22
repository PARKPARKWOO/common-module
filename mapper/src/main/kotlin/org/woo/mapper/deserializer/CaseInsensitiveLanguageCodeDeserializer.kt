package com.klleon.common.mapper.deserializer

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.klleon.common.language.LanguageCode

class CaseInsensitiveLanguageCodeDeserializer : JsonDeserializer<LanguageCode>() {
    companion object {
        val languageCodeMap = LanguageCode.values().associateBy { it.value }
    }

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): LanguageCode {
        val lowercaseCode = p.text.lowercase()
        return languageCodeMap[lowercaseCode] ?: throw IllegalArgumentException("unknown language code: ${p.text}")
    }
}
