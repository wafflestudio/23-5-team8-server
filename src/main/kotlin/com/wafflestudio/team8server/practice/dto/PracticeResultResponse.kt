package com.wafflestudio.team8server.practice.dto

data class PracticeResultResponse(
    val practiceLogId: Long,
    val practiceAt: String,
    val totalAttempts: Int,
    val successCount: Int,
    val attempts: List<PracticeAttemptResult>,
)
