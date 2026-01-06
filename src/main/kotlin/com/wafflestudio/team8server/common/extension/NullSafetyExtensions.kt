package com.wafflestudio.team8server.common.extension

import com.wafflestudio.team8server.common.exception.ResourceNotFoundException

// !! 대신 NullSafety 보장하는 확장 함수
fun <T> T?.ensureNotNull(message: String = "데이터가 존재하지 않습니다"): T = this ?: throw ResourceNotFoundException(message)
