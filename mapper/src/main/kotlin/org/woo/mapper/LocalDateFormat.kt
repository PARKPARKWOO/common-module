package org.woo.mapper

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object LocalDateFormat {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun parse(str: String?): LocalDate? {
        return str?.let {
            LocalDate.parse(str, formatter)
        }
    }
}
