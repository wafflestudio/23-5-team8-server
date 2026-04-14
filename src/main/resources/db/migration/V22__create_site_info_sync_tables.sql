CREATE TABLE IF NOT EXISTS sync_with_site_settings (
                                                       id BIGINT PRIMARY KEY,
                                                       enabled BOOLEAN NOT NULL DEFAULT FALSE,
                                                       updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='사이트(수강신청) 자동 동기화 설정(single row)';

INSERT INTO sync_with_site_settings (id, enabled)
VALUES (1, FALSE)
    ON DUPLICATE KEY UPDATE enabled = enabled;

CREATE TABLE IF NOT EXISTS sync_with_site_runs (
                                                   id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                   status VARCHAR(20) NOT NULL COMMENT 'SUCCESS | FAILED',
    started_at DATETIME(6) NOT NULL,
    finished_at DATETIME(6) NULL,
    dumped_data LONGTEXT NULL COMMENT '크롤링 결과 JSON 덤프 데이터',
    message VARCHAR(1000) NULL COMMENT '에러 또는 실행 결과 메시지'
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='사이트(수강신청) 동기화 실행 이력';

CREATE INDEX idx_sync_with_site_runs_started_at ON sync_with_site_runs (started_at);