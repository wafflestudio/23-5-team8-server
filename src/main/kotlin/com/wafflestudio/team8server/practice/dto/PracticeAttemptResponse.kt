package com.wafflestudio.team8server.practice.dto

data class PracticeAttemptResponse(
    val isSuccess: Boolean,
    val message: String,
    val userLatencyMs: Int,
)
