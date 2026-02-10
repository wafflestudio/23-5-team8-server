-- 일별 신규 가입자 집계 최적화를 위한 index
CREATE INDEX idx_users_created_at
    ON users (created_at);

-- DAU / 일별 연습 시도 집계 최적화를 위한 index
CREATE INDEX idx_practice_logs_practice_at
    ON practice_logs (practice_at);