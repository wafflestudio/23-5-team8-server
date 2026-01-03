CREATE TABLE IF NOT EXISTS pre_enrolls (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT NOT NULL COMMENT '유저 id',
    course_id  BIGINT NOT NULL COMMENT '강의 id',
    cart_count INT    NOT NULL DEFAULT 0 COMMENT '담은 사람 수',

    -- 유저 삭제 시 장바구니 내역 함께 삭제
    CONSTRAINT fk_pre_enrolls_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,

    -- 강의 삭제 시 장바구니 내역 함께 삭제
    CONSTRAINT fk_pre_enrolls_course FOREIGN KEY (course_id)
        REFERENCES courses (id) ON DELETE CASCADE,

    -- 한 유저가 같은 강의 중복 담기 방지 (이것이 인덱스 역할도 수행)
    CONSTRAINT uk_pre_enrolls_user_course UNIQUE (user_id, course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='장바구니';