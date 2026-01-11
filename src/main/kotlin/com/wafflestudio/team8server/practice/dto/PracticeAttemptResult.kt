package com.wafflestudio.team8server.practice.dto

data class PracticeAttemptResult(
    val courseId: Long?,
    val courseTitle: String?,
    val isSuccess: Boolean,
    val rank: Int,
    val percentile: Double,
    val reactionTime: Int,
)
