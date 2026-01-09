package com.wafflestudio.team8server.practice.dto

data class PracticeStartResponse(
    val practiceLogId: Long,
    val virtualStartTime: String,
    val targetTime: String,
    val timeLimit: String,
    val message: String,
)
