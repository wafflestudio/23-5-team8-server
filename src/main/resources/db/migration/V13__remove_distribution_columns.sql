-- Remove distribution_scale and distribution_shape columns from practice_details table
-- These columns are no longer needed as percentile calculation is now based on actual DB data

ALTER TABLE practice_details DROP COLUMN distribution_scale;
ALTER TABLE practice_details DROP COLUMN distribution_shape;
