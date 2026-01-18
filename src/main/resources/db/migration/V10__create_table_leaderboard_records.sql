CREATE TABLE IF NOT EXISTS leaderboard_records (
    user_id                      BIGINT  NOT NULL,
    best_first_reaction_time     INT     NULL COMMENT '첫번째 수강신청 최단 반응시간(ms)',
    best_second_reaction_time    INT     NULL COMMENT '두번째 수강신청 최단 반응시간(ms)',
    best_competition_rate        DOUBLE  NULL COMMENT '성공한 최고 경쟁률 (total_competitors/capacity)',

    CONSTRAINT pk_leaderboard_records PRIMARY KEY (user_id),
    CONSTRAINT fk_leaderboard_records_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
COMMENT = '유저별 리더보드 등재 기록';

CREATE INDEX idx_leaderboard_records_best_first_reaction_time
    ON leaderboard_records (best_first_reaction_time);

CREATE INDEX idx_leaderboard_records_best_second_reaction_time
    ON leaderboard_records (best_second_reaction_time);

CREATE INDEX idx_leaderboard_records_best_competition_rate
    ON leaderboard_records (best_competition_rate);