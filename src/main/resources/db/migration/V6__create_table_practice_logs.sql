CREATE TABLE IF NOT EXISTS practice_logs (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT   NOT NULL COMMENT '유저 id',
    practice_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '연습 시작 시점',

    CONSTRAINT fk_practice_logs_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='수강신청 연습 기록';