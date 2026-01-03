CREATE TABLE IF NOT EXISTS courses (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    year            INT          NOT NULL COMMENT '개설연도',
    semester        ENUM('SPRING', 'SUMMER', 'FALL', 'WINTER') NOT NULL COMMENT '개설학기',
    classification  VARCHAR(50)  NULL     COMMENT '교과구분',
    college         VARCHAR(100) NULL     COMMENT '개설대학',
    department      VARCHAR(100) NULL     COMMENT '개설학과',
    academic_course VARCHAR(50)  NULL     COMMENT '이수과정',
    academic_year   VARCHAR(20)  NULL     COMMENT '학년',
    course_number   VARCHAR(20)  NOT NULL COMMENT '교과목번호',
    lecture_number  VARCHAR(10)  NOT NULL COMMENT '강좌번호',
    course_title    VARCHAR(255) NOT NULL COMMENT '교과목명',
    credit          INT          NULL     COMMENT '학점수',
    instructor      VARCHAR(100) NULL     COMMENT '담당교수',
    place_and_time  JSON         NULL     COMMENT '수업시간 및 강의실정보',
    quota           INT          NOT NULL COMMENT '정원',
    freshman_quota  INT          NULL     COMMENT '신입생정원',

    CONSTRAINT uk_courses_unique_lecture UNIQUE (year, semester, course_number, lecture_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='개설 강의 정보';

-- 인덱스
-- 연도/학기 복합 인덱스 (조회 시 Cardinality가 가장 높고 기본이 되는 필터)
CREATE INDEX idx_courses_year_semester ON courses (year, semester);
-- 담당 교수명 인덱스
CREATE INDEX idx_courses_instructor ON courses (instructor);