package com.wafflestudio.team8server.user.service

import com.wafflestudio.team8server.common.exception.UserNotFoundException
import com.wafflestudio.team8server.user.dto.PresignedUrlRequest
import com.wafflestudio.team8server.user.dto.PresignedUrlResponse
import com.wafflestudio.team8server.user.dto.UpdateProfileImageRequest
import com.wafflestudio.team8server.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val s3Service: S3Service,
) {
    fun generatePresignedUrl(
        userId: Long,
        request: PresignedUrlRequest,
    ): PresignedUrlResponse {
        val presignedUrl =
            s3Service.generatePresignedUrl(
                userId = userId,
                extension = request.extension,
                contentType = request.contentType,
            )
        return PresignedUrlResponse(presignedUrl = presignedUrl)
    }

    @Transactional
    fun updateProfileImage(
        userId: Long,
        request: UpdateProfileImageRequest,
    ) {
        val user =
            userRepository.findById(userId).orElseThrow {
                UserNotFoundException(userId)
            }

        // 기존 이미지가 있으면 S3에서 삭제
        user.profileImageUrl?.let { existingKey ->
            s3Service.deleteObject(existingKey)
        }

        // 새 이미지 URL에서 key 추출 후 저장
        val newKey = s3Service.extractKeyFromUrl(request.imageUrl)
        user.profileImageUrl = newKey
    }

    @Transactional
    fun deleteProfileImage(userId: Long) {
        val user =
            userRepository.findById(userId).orElseThrow {
                UserNotFoundException(userId)
            }

        // 기존 이미지가 있으면 S3에서 삭제
        user.profileImageUrl?.let { existingKey ->
            s3Service.deleteObject(existingKey)
        }

        user.profileImageUrl = null
    }

    fun getProfileImageUrl(userId: Long): String? {
        val user =
            userRepository.findById(userId).orElseThrow {
                UserNotFoundException(userId)
            }

        return user.profileImageUrl?.let { key ->
            s3Service.buildFullUrl(key)
        }
    }
}
