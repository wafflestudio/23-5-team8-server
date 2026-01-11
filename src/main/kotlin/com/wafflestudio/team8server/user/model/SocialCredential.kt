package com.wafflestudio.team8server.user.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "social_credentials",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_provider_social_id", columnNames = ["provider", "social_id"]),
    ],
)
class SocialCredential(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,
    @Column(nullable = false, length = 20)
    val provider: String, // provider에 따라 "kakao" or "google"
    @Column(name = "social_id", nullable = false, length = 255)
    val socialId: String,
) : BaseEntity()
