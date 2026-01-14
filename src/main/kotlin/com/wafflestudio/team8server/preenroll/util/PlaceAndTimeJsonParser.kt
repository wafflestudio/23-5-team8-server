package com.wafflestudio.team8server.preenroll.util

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

/**
 * [com.wafflestudio.team8server.course.model.Course.placeAndTime]에서 필드를 추출합니다.
 */

object PlaceAndTimeJsonParser {
    fun extractTime(
        objectMapper: ObjectMapper,
        placeAndTimeJson: String?,
    ): String? {
        if (placeAndTimeJson.isNullOrBlank()) return null

        val node = readTreeSafely(objectMapper, placeAndTimeJson) ?: return null
        val timeNode = node.get("time") ?: return null

        return timeNode.asText().trim().ifBlank { null }
    }

    private fun readTreeSafely(
        objectMapper: ObjectMapper,
        rawJson: String,
    ): JsonNode? =
        try {
            objectMapper.readTree(rawJson)
        } catch (e: Exception) {
            null
        }
}
