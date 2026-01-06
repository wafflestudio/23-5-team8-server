package com.wafflestudio.team8server.user.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "local_credentials",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_local_email", columnNames = ["email"]),
        UniqueConstraint(name = "uk_local_user_id", columnNames = ["user_id"]),
    ],
)
class LocalCredential(
    // 지연 로딩
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,
    // 이메일 중복 방지
    @Column(nullable = false, unique = true)
    val email: String,
    // BCrypt 해시값 (60자), 비밀번호 변경을 위해 var
    @Column(nullable = false)
    var passwordHash: String,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
) : BaseEntity()
