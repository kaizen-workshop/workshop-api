ALTER TABLE workshop.app_user ADD COLUMN token_version INTEGER NOT NULL DEFAULT 0;
ALTER TABLE workshop.app_user ADD CONSTRAINT app_user_token_version_nonnegative CHECK (token_version >= 0);
