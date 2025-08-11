package com.stark.shoot.infrastructure.util

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class ParticipantsConverter : AttributeConverter<List<Long>, String> {

    private val mapper = jacksonObjectMapper()

    override fun convertToDatabaseColumn(attribute: List<Long>?): String =
        attribute?.let { mapper.writeValueAsString(it) } ?: "[]"

    override fun convertToEntityAttribute(dbData: String?): List<Long> =
        dbData?.let { mapper.readValue(it) } ?: emptyList()

}
