package com.wafflestudio.team8server.course.sync

import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.io.InputStream

class ByteArrayMultipartFile(
    private val bytes: ByteArray,
    private val name: String,
    private val originalFilename: String,
    private val contentType: String? = "application/vnd.ms-excel",
) : MultipartFile {
    override fun getName(): String = name

    override fun getOriginalFilename(): String = originalFilename

    override fun getContentType(): String? = contentType

    override fun isEmpty(): Boolean = bytes.isEmpty()

    override fun getSize(): Long = bytes.size.toLong()

    override fun getBytes(): ByteArray = bytes

    override fun getInputStream(): InputStream = ByteArrayInputStream(bytes)

    override fun transferTo(dest: java.io.File) = dest.writeBytes(bytes)
}
