CREATE TABLE IF NOT EXISTS practice_details (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    log_id           BIGINT  NOT NULL COMMENT '로그 id',
    course_id        BIGINT  NULL     COMMENT '강의 id (삭제된 경우 NULL)',
    is_success       BOOLEAN NOT NULL COMMENT '성공/실패',
    reaction_time    INT     NOT NULL COMMENT '반응속도(ms)',
    early_click_diff INT     NULL     COMMENT '일찍 누른 시간(ms)',

    -- 로그 헤더(practice_log) 삭제 시 상세 내용 삭제
    CONSTRAINT fk_practice_details_log FOREIGN KEY (log_id)
        REFERENCES practice_logs (id) ON DELETE CASCADE,

    -- 강의(course)가 삭제되면 이 컬럼을 NULL로 변경하여 기록 보존 (SET NULL)
    CONSTRAINT fk_practice_details_course FOREIGN KEY (course_id)
        REFERENCES courses (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='수강신청 연습 상세';