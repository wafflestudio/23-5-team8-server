CREATE TABLE IF NOT EXISTS course_sync_settings (
    id BIGINT PRIMARY KEY,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='강의 자동 동기화 설정(single row)';

INSERT INTO course_sync_settings (id, enabled)
VALUES (1, FALSE)
ON DUPLICATE KEY UPDATE enabled = enabled;

CREATE TABLE IF NOT EXISTS course_sync_runs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    status VARCHAR(20) NOT NULL COMMENT 'SUCCESS | FAILED',
    started_at DATETIME(6) NOT NULL,
    finished_at DATETIME(6) NULL,
    year INT NOT NULL,
    semester ENUM('SPRING', 'SUMMER', 'FALL', 'WINTER') NOT NULL,
    rows_upserted INT NULL,
    message VARCHAR(500) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='최근 강의 동기화 실행 이력';

CREATE INDEX idx_course_sync_runs_started_at ON course_sync_runs (started_at);