ALTER TABLE social_credentials
    ADD COLUMN refresh_token VARCHAR(2048) NULL AFTER social_id;