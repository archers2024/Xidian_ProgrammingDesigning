-- Existing databases: apply once; do not re-import init.sql.
ALTER TABLE t_admin ADD COLUMN token_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE t_admin ADD COLUMN must_change_password TINYINT NOT NULL DEFAULT 0;
