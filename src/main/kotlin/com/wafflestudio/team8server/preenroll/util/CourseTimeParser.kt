package com.wafflestudio.team8server.preenroll.util

/**
 * course time string을 파싱합니다.
 *
 * - course time 형식: "<시간대1>/<시간대2>/..."
 * - 각 시간대: "<요일>(HH:MM~HH:MM)"
 * - example: "수(19:00~21:50)"
 * - example: "목(18:00~19:50)/목(20:00~21:50)"
 */
object CourseTimeParser {
    private val blockRegex =
        Regex(
            pattern = "^([월화수목금토일])\\s*\\(\\s*(\\d{1,2}):(\\d{2})\\s*~\\s*(\\d{1,2}):(\\d{2})\\s*\\)\\s*$",
        )

    fun parseToTimeSlots(raw: String?): List<TimeSlot> {
        if (raw.isNullOrBlank()) return emptyList()

        val slots = mutableListOf<TimeSlot>()
        val parts = raw.split('/')
        for (part in parts) {
            val token = part.trim()
            if (token.isEmpty()) continue

            val match = blockRegex.matchEntire(token) ?: continue

            val dayChar = match.groupValues[1].firstOrNull() ?: continue
            val day = KoreanDayOfWeek.fromChar(dayChar) ?: continue

            val sh = match.groupValues[2].toIntOrNull() ?: continue
            val sm = match.groupValues[3].toIntOrNull() ?: continue
            val eh = match.groupValues[4].toIntOrNull() ?: continue
            val em = match.groupValues[5].toIntOrNull() ?: continue

            if (!isValidHourMinute(sh, sm) || !isValidHourMinute(eh, em)) continue

            val startMinute = sh * 60 + sm
            val endMinute = eh * 60 + em
            if (endMinute <= startMinute) continue

            slots.add(
                TimeSlot(
                    day = day,
                    startMinute = startMinute,
                    endMinute = endMinute,
                ),
            )
        }
        return slots
    }

    fun isConflict(
        a: List<TimeSlot>,
        b: List<TimeSlot>,
    ): Boolean {
        if (a.isEmpty() || b.isEmpty()) return false

        for (x in a) {
            for (y in b) {
                if (x.day != y.day) continue
                if (overlaps(x.startMinute, x.endMinute, y.startMinute, y.endMinute)) return true
            }
        }
        return false
    }

    private fun isValidHourMinute(
        hour: Int,
        minute: Int,
    ): Boolean = hour in 0..23 && minute in 0..59

    private fun overlaps(
        s1: Int,
        e1: Int,
        s2: Int,
        e2: Int,
    ): Boolean = s1 < e2 && s2 < e1
}

enum class KoreanDayOfWeek(
    val symbol: Char,
) {
    MONDAY('월'),
    TUESDAY('화'),
    WEDNESDAY('수'),
    THURSDAY('목'),
    FRIDAY('금'),
    SATURDAY('토'),
    SUNDAY('일'),
    ;

    companion object {
        fun fromChar(c: Char): KoreanDayOfWeek? = entries.firstOrNull { it.symbol == c }
    }
}

data class TimeSlot(
    val day: KoreanDayOfWeek,
    val startMinute: Int,
    val endMinute: Int,
)
