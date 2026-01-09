package com.wafflestudio.team8server.practice.dto

data class PracticeAttemptResponse(
    val isSuccess: Boolean,
    val message: String,
    val rank: Int? = null,
    val percentile: Double? = null,
    val reactionTime: Int,
    val earlyClickDiff: Int? = null,
)
