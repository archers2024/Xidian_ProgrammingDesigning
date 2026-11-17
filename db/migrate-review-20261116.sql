ALTER TABLE t_organization ADD COLUMN require_review TINYINT NOT NULL DEFAULT 1;
UPDATE t_organization SET require_review=0 WHERE org_id=1;
ALTER TABLE t_activity ADD COLUMN approval_status VARCHAR(20) NOT NULL DEFAULT 'NONE';
CREATE TABLE t_activity_review (
 review_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 activity_id BIGINT NOT NULL,
 org_id BIGINT NOT NULL,
 submitted_by BIGINT NOT NULL,
 reviewer_id BIGINT DEFAULT NULL,
 decision VARCHAR(20) NOT NULL DEFAULT 'PENDING',
 comment VARCHAR(500) DEFAULT NULL,
 submitted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 reviewed_at DATETIME DEFAULT NULL,
 INDEX idx_review_queue(decision,org_id,submitted_at),
 INDEX idx_review_activity(activity_id,review_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
