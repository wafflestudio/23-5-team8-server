package com.wafflestudio.team8server.user.repository

import com.wafflestudio.team8server.user.model.SocialCredential
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

interface SocialCredentialRepository : JpaRepository<SocialCredential, Long> {
    fun findByProviderIdAndSocialId(provider: String, socialId: String): SocialCredential?
    fun existsByProviderIdAndSocialId(provider: String, socialId: String): Boolean
}