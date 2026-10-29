-- 达梦 DM8 建表脚本（至少第三范式）。
-- 本机默认运行库是 H2，结构以 backend/src/main/resources/db/schema.sql 为准，并与本脚本保持一致。
-- 在空库中执行一次。时间列保存 UTC 时间戳，接口层转换为 Asia/Shanghai。
-- 主键由应用生成 UUID，不使用自增列。

CREATE TABLE admin_account (
    id VARCHAR(36) NOT NULL,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(64) NOT NULL,
    enabled BIT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_admin_account PRIMARY KEY (id),
    CONSTRAINT uq_admin_username UNIQUE (username)
);

CREATE TABLE activity_type (
    id VARCHAR(36) NOT NULL,
    name VARCHAR(40) NOT NULL,
    sort_order INT NOT NULL,
    enabled BIT NOT NULL,
    CONSTRAINT pk_activity_type PRIMARY KEY (id),
    CONSTRAINT uq_activity_type_name UNIQUE (name)
);

CREATE TABLE college (
    id VARCHAR(36) NOT NULL,
    name VARCHAR(80) NOT NULL,
    sort_order INT NOT NULL,
    enabled BIT NOT NULL,
    CONSTRAINT pk_college PRIMARY KEY (id),
    CONSTRAINT uq_college_name UNIQUE (name)
);

CREATE TABLE activity (
    id VARCHAR(36) NOT NULL,
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
    CONSTRAINT pk_activity PRIMARY KEY (id),
    CONSTRAINT fk_activity_type FOREIGN KEY (type_id) REFERENCES activity_type (id),
    CONSTRAINT fk_activity_admin FOREIGN KEY (created_by) REFERENCES admin_account (id)
);

CREATE INDEX idx_activity_type_start ON activity (type_id, start_at);
CREATE INDEX idx_activity_deleted ON activity (deleted_at);

CREATE TABLE registration (
    id VARCHAR(36) NOT NULL,
    activity_id VARCHAR(36) NOT NULL,
    participant_name VARCHAR(30) NOT NULL,
    phone VARCHAR(11) NOT NULL,
    college_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_registration PRIMARY KEY (id),
    CONSTRAINT uq_registration_phone UNIQUE (activity_id, phone),
    CONSTRAINT fk_registration_activity FOREIGN KEY (activity_id) REFERENCES activity (id),
    CONSTRAINT fk_registration_college FOREIGN KEY (college_id) REFERENCES college (id)
);

CREATE INDEX idx_registration_activity ON registration (activity_id);

CREATE TABLE attachment (
    id VARCHAR(36) NOT NULL,
    activity_id VARCHAR(36) NOT NULL,
    original_name VARCHAR(200) NOT NULL,
    storage_key VARCHAR(80) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    size_bytes BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_attachment PRIMARY KEY (id),
    CONSTRAINT fk_attachment_activity FOREIGN KEY (activity_id) REFERENCES activity (id)
);

CREATE INDEX idx_attachment_activity ON attachment (activity_id);

CREATE TABLE checkin_session (
    id VARCHAR(36) NOT NULL,
    activity_id VARCHAR(36) NOT NULL,
    session_mode VARCHAR(16) NOT NULL,
    window_start TIMESTAMP NOT NULL,
    window_end TIMESTAMP NOT NULL,
    token_version INT NOT NULL,
    enabled BIT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_checkin_session PRIMARY KEY (id),
    CONSTRAINT fk_checkin_session_activity FOREIGN KEY (activity_id) REFERENCES activity (id)
);

CREATE INDEX idx_checkin_session_activity ON checkin_session (activity_id, enabled);

CREATE TABLE checkin_record (
    registration_id VARCHAR(36) NOT NULL,
    session_id VARCHAR(36) NOT NULL,
    checked_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_checkin_record PRIMARY KEY (registration_id),
    CONSTRAINT fk_checkin_record_registration FOREIGN KEY (registration_id) REFERENCES registration (id),
    CONSTRAINT fk_checkin_record_session FOREIGN KEY (session_id) REFERENCES checkin_session (id)
);
