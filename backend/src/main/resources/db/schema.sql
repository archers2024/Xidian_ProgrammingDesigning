-- 运行库表结构。默认由 H2 执行；达梦脚本见 database/dm/schema.sql。
-- 时间列保存 UTC 时间戳，接口层转换为 Asia/Shanghai。

CREATE TABLE IF NOT EXISTS admin_account (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(64) NOT NULL,
    enabled BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_admin_username UNIQUE (username)
);

CREATE TABLE IF NOT EXISTS activity_type (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    name VARCHAR(40) NOT NULL,
    sort_order INT NOT NULL,
    enabled BOOLEAN NOT NULL,
    CONSTRAINT uq_activity_type_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS college (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    sort_order INT NOT NULL,
    enabled BOOLEAN NOT NULL,
    CONSTRAINT uq_college_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS activity (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    type_id VARCHAR(36) NOT NULL,
    name VARCHAR(80) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    location VARCHAR(120) NOT NULL,
    start_at TIMESTAMP NOT NULL,
    end_at TIMESTAMP NOT NULL,
    registration_deadline TIMESTAMP NOT NULL,
    capacity INT NOT NULL,
    publication_state VARCHAR(16) NOT NULL,
    published_at TIMESTAMP,
    live_url VARCHAR(500),
    cover_storage_key VARCHAR(80),
    cover_content_type VARCHAR(80),
    created_by VARCHAR(36) NOT NULL,
    version INT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT fk_activity_type FOREIGN KEY (type_id) REFERENCES activity_type (id),
    CONSTRAINT fk_activity_admin FOREIGN KEY (created_by) REFERENCES admin_account (id)
);

CREATE INDEX IF NOT EXISTS idx_activity_type_start ON activity (type_id, start_at);
CREATE INDEX IF NOT EXISTS idx_activity_deleted ON activity (deleted_at);

CREATE TABLE IF NOT EXISTS registration (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    activity_id VARCHAR(36) NOT NULL,
    participant_name VARCHAR(30) NOT NULL,
    phone VARCHAR(11) NOT NULL,
    college_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_registration_phone UNIQUE (activity_id, phone),
    CONSTRAINT fk_registration_activity FOREIGN KEY (activity_id) REFERENCES activity (id),
    CONSTRAINT fk_registration_college FOREIGN KEY (college_id) REFERENCES college (id)
);

CREATE INDEX IF NOT EXISTS idx_registration_activity ON registration (activity_id);

CREATE TABLE IF NOT EXISTS attachment (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    activity_id VARCHAR(36) NOT NULL,
    original_name VARCHAR(200) NOT NULL,
    storage_key VARCHAR(80) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    size_bytes BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_attachment_activity FOREIGN KEY (activity_id) REFERENCES activity (id)
);

CREATE INDEX IF NOT EXISTS idx_attachment_activity ON attachment (activity_id);

CREATE TABLE IF NOT EXISTS checkin_session (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    activity_id VARCHAR(36) NOT NULL,
    session_mode VARCHAR(16) NOT NULL,
    window_start TIMESTAMP NOT NULL,
    window_end TIMESTAMP NOT NULL,
    token_version INT NOT NULL,
    enabled BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_checkin_session_activity FOREIGN KEY (activity_id) REFERENCES activity (id)
);

CREATE INDEX IF NOT EXISTS idx_checkin_session_activity ON checkin_session (activity_id, enabled);

CREATE TABLE IF NOT EXISTS checkin_record (
    registration_id VARCHAR(36) NOT NULL PRIMARY KEY,
    session_id VARCHAR(36) NOT NULL,
    checked_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_checkin_record_registration FOREIGN KEY (registration_id) REFERENCES registration (id),
    CONSTRAINT fk_checkin_record_session FOREIGN KEY (session_id) REFERENCES checkin_session (id)
);
