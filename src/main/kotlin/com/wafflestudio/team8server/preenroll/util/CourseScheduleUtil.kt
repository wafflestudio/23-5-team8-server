package com.wafflestudio.team8server.preenroll.util

import com.fasterxml.jackson.databind.ObjectMapper

/**
 * Course.placeAndTime JSON을 time slot으로 변환하고 충돌 여부를 확인합니다.
 */
class CourseScheduleUtil {
    fun extractTimeSlots(
        objectMapper: ObjectMapper,
        placeAndTimeJson: String?,
    ): List<TimeSlot> {
        val time = PlaceAndTimeJsonParser.extractTime(objectMapper, placeAndTimeJson)
        return CourseTimeParser.parseToTimeSlots(time)
    }

    fun hasTimeConflict(
        objectMapper: ObjectMapper,
        placeAndTimeJsonA: String?,
        placeAndTimeJsonB: String?,
    ): Boolean {
        val a = extractTimeSlots(objectMapper, placeAndTimeJsonA)
        val b = extractTimeSlots(objectMapper, placeAndTimeJsonB)
        return CourseTimeParser.isConflict(a, b)
    }
}
