ALTER TABLE practice_details
    ADD COLUMN user_rank INT NULL COMMENT '등수 (조기 클릭 시 NULL)',
    ADD COLUMN percentile DOUBLE NULL COMMENT '백분위 (조기 클릭 시 NULL)',
    ADD COLUMN capacity INT NOT NULL DEFAULT 0 COMMENT '수강 정원',
    ADD COLUMN total_competitors INT NOT NULL DEFAULT 0 COMMENT '전체 경쟁자 수',
    ADD COLUMN distribution_scale DOUBLE NOT NULL DEFAULT 0.0 COMMENT '로그정규분포 scale 파라미터 (μ)',
    ADD COLUMN distribution_shape DOUBLE NOT NULL DEFAULT 0.0 COMMENT '로그정규분포 shape 파라미터 (σ)';
