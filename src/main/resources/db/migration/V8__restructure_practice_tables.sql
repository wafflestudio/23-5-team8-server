-- Step 1: practice_details에 통계 컬럼 추가
ALTER TABLE practice_details
    ADD COLUMN user_rank INT NOT NULL DEFAULT 0 COMMENT '등수',
    ADD COLUMN percentile DOUBLE NOT NULL DEFAULT 0.0 COMMENT '백분위',
    ADD COLUMN capacity INT NOT NULL DEFAULT 0 COMMENT '수강 정원',
    ADD COLUMN total_competitors INT NOT NULL DEFAULT 0 COMMENT '전체 경쟁자 수',
    ADD COLUMN distribution_scale DOUBLE NOT NULL DEFAULT 0.0 COMMENT '로그정규분포 scale 파라미터 (μ)',
    ADD COLUMN distribution_shape DOUBLE NOT NULL DEFAULT 0.0 COMMENT '로그정규분포 shape 파라미터 (σ)';

-- Step 2: early_click_diff를 practice_details에서 practice_logs로 이동
-- 이유: early click은 세션당 하나만 기록되며, 개별 과목 시도와는 무관함
ALTER TABLE practice_logs
    ADD COLUMN early_click_diff INT NULL COMMENT '일찍 클릭한 시간 (ms, 음수값)';

ALTER TABLE practice_details
    DROP COLUMN early_click_diff;

-- Step 3: 한 세션에서 각 과목당 하나의 레코드만 허용 (중복 방지)
ALTER TABLE practice_details
    ADD CONSTRAINT uk_practice_details_log_course UNIQUE (log_id, course_id);
