package com.wafflestudio.team8server.user.repository

import com.wafflestudio.team8server.user.model.LocalCredential
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface LocalCredentialRepository : JpaRepository<LocalCredential, Long> {
    // 이메일 중복 확인 (true: 존재함, false: 없음)
    fun existsByEmail(email: String): Boolean

    // 이메일로 인증 정보 조회 (없으면 null 반환)
    fun findByEmail(email: String): LocalCredential?
}
