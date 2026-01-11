package com.wafflestudio.team8server.user

import com.wafflestudio.team8server.common.extension.ensureNotNull
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    @Value("\${jwt.secret}")
    private val secretKey: String,
    @Value("\${jwt.expiration-in-ms}")
    private val expirationInMs: Long,
) {
    // HMAC-SHA256 알고리즘을 위한 비밀키 생성 (lazy initialization)
    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(secretKey.toByteArray())
    }

    fun createToken(
        userId: Long?,
        // email: String,
    ): String {
        val now = Date()
        val expiryDate = Date(now.time + expirationInMs)

        return Jwts
            .builder()
            .subject(userId.ensureNotNull().toString()) // 토큰의 주체(subject): 사용자 ID
            // .claim("email", email) // 추가 정보: 이메일
            .issuedAt(now) // 토큰 발급 시간
            .expiration(expiryDate) // 토큰 만료 시간
            .signWith(key) // HMAC-SHA256으로 서명
            .compact() // 최종 JWT 문자열 생성
    }

    fun getUserIdFromToken(token: String): Long {
        val claims = parseClaims(token)
        return claims.subject.toLong()
    }

    fun validateToken(token: String): Boolean =
        try {
            parseClaims(token) // 파싱 성공 시 유효한 토큰
            true
        } catch (e: Exception) {
            // 토큰이 유효하지 않거나 만료됨
            false
        }

    fun getRemainingExpirationTime(token: String): Long {
        val claims = parseClaims(token)
        val expirationDate = claims.expiration
        val now = Date()

        return if (expirationDate.after(now)) {
            expirationDate.time - now.time
        } else {
            0L
        }
    }

    private fun parseClaims(token: String): Claims =
        Jwts
            .parser()
            .verifyWith(key) // 서명 검증
            .build()
            .parseSignedClaims(token) // 토큰 파싱
            .payload // Claims 반환
}
