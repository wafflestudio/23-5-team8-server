package com.wafflestudio.team8server.user.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "users")
class User(
    @Column(nullable = false, length = 50)
    var nickname: String,
    @Column(length = 255)
    var profileImageUrl: String? = null,
    @Id
    // DB가 자동으로 ID 생성
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
) : BaseEntity()
