-- leaderboard_records 테이블에 달성 시간 컬럼 추가
ALTER TABLE leaderboard_records
    ADD COLUMN best_first_reaction_time_achieved_at DATETIME NULL COMMENT '1픽 최단 반응시간 달성 시각',
    ADD COLUMN best_second_reaction_time_achieved_at DATETIME NULL COMMENT '2픽 최단 반응시간 달성 시각',
    ADD COLUMN best_competition_rate_achieved_at DATETIME NULL COMMENT '최고 경쟁률 달성 시각';

-- weekly_leaderboard_records 테이블에 달성 시간 컬럼 추가
ALTER TABLE weekly_leaderboard_records
    ADD COLUMN best_first_reaction_time_achieved_at DATETIME NULL COMMENT '1픽 최단 반응시간 달성 시각',
    ADD COLUMN best_second_reaction_time_achieved_at DATETIME NULL COMMENT '2픽 최단 반응시간 달성 시각',
    ADD COLUMN best_competition_rate_achieved_at DATETIME NULL COMMENT '최고 경쟁률 달성 시각';

-- 기존 인덱스 삭제 후 복합 인덱스로 재생성 (정렬 안정성 보장)
DROP INDEX idx_leaderboard_records_best_first_reaction_time ON leaderboard_records;
DROP INDEX idx_leaderboard_records_best_second_reaction_time ON leaderboard_records;
DROP INDEX idx_leaderboard_records_best_competition_rate ON leaderboard_records;

CREATE INDEX idx_leaderboard_records_best_first_reaction_time
    ON leaderboard_records (best_first_reaction_time ASC, best_first_reaction_time_achieved_at ASC);
CREATE INDEX idx_leaderboard_records_best_second_reaction_time
    ON leaderboard_records (best_second_reaction_time ASC, best_second_reaction_time_achieved_at ASC);
CREATE INDEX idx_leaderboard_records_best_competition_rate
    ON leaderboard_records (best_competition_rate DESC, best_competition_rate_achieved_at ASC);

DROP INDEX idx_weekly_leaderboard_records_best_first_reaction_time ON weekly_leaderboard_records;
DROP INDEX idx_weekly_leaderboard_records_best_second_reaction_time ON weekly_leaderboard_records;
DROP INDEX idx_weekly_leaderboard_records_best_competition_rate ON weekly_leaderboard_records;

CREATE INDEX idx_weekly_leaderboard_records_best_first_reaction_time
    ON weekly_leaderboard_records (best_first_reaction_time ASC, best_first_reaction_time_achieved_at ASC);
CREATE INDEX idx_weekly_leaderboard_records_best_second_reaction_time
    ON weekly_leaderboard_records (best_second_reaction_time ASC, best_second_reaction_time_achieved_at ASC);
CREATE INDEX idx_weekly_leaderboard_records_best_competition_rate
    ON weekly_leaderboard_records (best_competition_rate DESC, best_competition_rate_achieved_at ASC);
