package com.wafflestudio.team8server.course.service

import com.wafflestudio.team8server.common.dto.PageInfo
import com.wafflestudio.team8server.course.dto.CourseSearchRequest
import com.wafflestudio.team8server.course.dto.CourseSearchResponse
import com.wafflestudio.team8server.course.model.Semester
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

@Service
class CourseService {

    fun search(request: CourseSearchRequest): CourseSearchResponse {
        return CourseSearchResponse(
            items = emptyList(),
            pageInfo = PageInfo(
                page = request.page,
                size = request.size,
                totalElements = 0,
                totalPages = 0,
                hasNext = false,
            ),
        )
    }

    fun import(year: Int, semester: Semester, file: MultipartFile) {
        throw UnsupportedOperationException("not implemented yet")
    }
}