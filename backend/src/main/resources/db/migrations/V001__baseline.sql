-- =====================================================================
-- 高校学生活动全流程管理系统  数据库初始化脚本（MySQL 8）
-- 设计满足第三范式：字典表独立、无传递依赖；冗余字段 signed_count 已声明
-- Owner：A岗（B/C/D 三岗只读调用，不得直接修改本文件已提交的脚本）
-- =====================================================================

CREATE TABLE IF NOT EXISTS t_organization (
 org_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 code VARCHAR(40) NOT NULL UNIQUE,
 name VARCHAR(100) NOT NULL,
 status TINYINT NOT NULL DEFAULT 1,
 require_review TINYINT NOT NULL DEFAULT 1,
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT IGNORE INTO t_organization(org_id,code,name) VALUES(1,'DEFAULT','校级活动中心');
UPDATE t_organization SET require_review=0 WHERE org_id=1;

-- ---------------------------- 管理员（A岗） ----------------------------
CREATE TABLE IF NOT EXISTS t_admin (
  admin_id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  username         VARCHAR(50)  NOT NULL COMMENT '登录账号',
  org_id BIGINT NOT NULL DEFAULT 1,
  password_hash    VARCHAR(100) NOT NULL COMMENT 'BCrypt 密文，禁止明文',
  token_version    BIGINT NOT NULL DEFAULT 0 COMMENT '令牌撤销版本',
  must_change_password TINYINT NOT NULL DEFAULT 0 COMMENT '临时密码需要修改',
  real_name        VARCHAR(50)  NOT NULL COMMENT '姓名',
  dept             VARCHAR(100) DEFAULT NULL COMMENT '所属部门',
  role_code        VARCHAR(20)  NOT NULL DEFAULT 'ADMIN' COMMENT 'ADMIN/SUPER',
  status           TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  last_login_time  DATETIME     DEFAULT NULL COMMENT '最近登录时间',
  create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (admin_id),
  UNIQUE KEY uk_admin_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员表';

-- -------------------- 活动类型字典（B岗） ------------------------------
CREATE TABLE IF NOT EXISTS t_activity_type (
  type_code   VARCHAR(20)  NOT NULL COMMENT '类型编码',
  type_name   VARCHAR(50)  NOT NULL COMMENT '类型名称',
  sort_order  INT          NOT NULL DEFAULT 0 COMMENT '排序',
  enabled     TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  PRIMARY KEY (type_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动类型字典表（消除类型名称的传递依赖）';

-- -------------------------- 活动（B岗） --------------------------------
CREATE TABLE IF NOT EXISTS t_activity (
  activity_id      BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  activity_no      VARCHAR(32)  NOT NULL COMMENT '活动编号（业务可见）',
  org_id BIGINT NOT NULL DEFAULT 1,
  signup_guid      VARCHAR(32)  NOT NULL COMMENT '报名条随机串，用于链接与二维码',
  title            VARCHAR(200) NOT NULL COMMENT '活动名称',
  type_code        VARCHAR(20)  NOT NULL COMMENT '活动类型，关联字典表',
  introduction     TEXT         COMMENT '活动简介',
  location         VARCHAR(200) NOT NULL COMMENT '活动地点',
  lng_lat          VARCHAR(50)  DEFAULT NULL COMMENT '地点经纬度，格式 lng,lat（准确签到校验用）',
  start_time       DATETIME     NOT NULL COMMENT '开始时间',
  end_time         DATETIME     NOT NULL COMMENT '结束时间',
  signup_deadline  DATETIME     NOT NULL COMMENT '报名截止时间（必须早于开始时间）',
  quota            INT          NOT NULL COMMENT '人数上限',
  signed_count     INT          NOT NULL DEFAULT 0 COMMENT '已报名人数（受控冗余，事务内维护）',
  status           VARCHAR(20)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/OPEN/CLOSED/ONGOING/FINISHED/CANCELED',
  approval_status VARCHAR(20) NOT NULL DEFAULT 'NONE',
  checkin_enabled  TINYINT      NOT NULL DEFAULT 0 COMMENT '是否开启签到',
  checkin_secret   VARCHAR(64)  DEFAULT NULL COMMENT '动态签到码密钥',
  checkin_radius   INT          NOT NULL DEFAULT 500 COMMENT '签到允许半径（米）',
  live_url         VARCHAR(500) DEFAULT NULL COMMENT '直播链接（可选）',
  cover_url        VARCHAR(500) DEFAULT NULL COMMENT '封面图地址',
  publish_time     DATETIME     DEFAULT NULL COMMENT '发布时间',
  created_by       BIGINT       NOT NULL COMMENT '创建人 admin_id',
  create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted          TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (activity_id),
  UNIQUE KEY uk_activity_no (activity_no),
  UNIQUE KEY uk_signup_guid (signup_guid),
  KEY idx_activity_status (status),
  KEY idx_activity_org (org_id,deleted,status),
  KEY idx_activity_type (type_code),
  KEY idx_activity_start (start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动表';

-- ------------------------ 活动附件（B岗） ------------------------------
CREATE TABLE IF NOT EXISTS t_attachment (
  att_id       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  activity_id  BIGINT       NOT NULL COMMENT '活动ID',
  file_name    VARCHAR(200) NOT NULL COMMENT '文件名',
  file_url     VARCHAR(500) NOT NULL COMMENT '文件地址',
  file_size    BIGINT       DEFAULT NULL COMMENT '文件大小（字节）',
  upload_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted      TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (att_id),
  KEY idx_att_activity (activity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动附件表';

-- --------------------- 学院字典（C岗） ---------------------------------
CREATE TABLE IF NOT EXISTS t_college (
  college_code  VARCHAR(20) NOT NULL COMMENT '学院代码',
  college_name  VARCHAR(100) NOT NULL COMMENT '学院名称',
  sort_order    INT NOT NULL DEFAULT 0,
  PRIMARY KEY (college_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学院字典表（消除学院名称的传递依赖）';

-- ------------------------ 报名记录（C岗） ------------------------------
CREATE TABLE IF NOT EXISTS t_registration (
  reg_id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  activity_id     BIGINT      NOT NULL COMMENT '活动ID',
  name            VARCHAR(50) NOT NULL COMMENT '姓名',
  phone           VARCHAR(20) NOT NULL COMMENT '手机号',
  college_code    VARCHAR(20) NOT NULL COMMENT '学院代码，关联字典表',
  source_channel  VARCHAR(20) DEFAULT NULL COMMENT '来源渠道 SCAN/LINK',
  signup_time     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '报名时间',
  status          VARCHAR(20) NOT NULL DEFAULT 'VALID' COMMENT 'VALID/CANCELED',
  deleted         TINYINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (reg_id),
  UNIQUE KEY uk_activity_phone (activity_id, phone) COMMENT '防止同一人重复报名',
  KEY idx_reg_activity (activity_id),
  KEY idx_reg_college (college_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报名记录表';

-- ------------------------ 签到记录（D岗） ------------------------------
CREATE TABLE IF NOT EXISTS t_checkin (
  checkin_id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  reg_id               BIGINT      NOT NULL COMMENT '对应报名记录',
  activity_id          BIGINT      NOT NULL COMMENT '活动ID（随报名确定，便于统计）',
  checkin_time         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '签到时间',
  method               VARCHAR(20) NOT NULL COMMENT 'BASIC基本 / ACCURATE准确',
  lng_lat              VARCHAR(50) DEFAULT NULL COMMENT '签到时上报坐标',
  device_fingerprint   VARCHAR(64) DEFAULT NULL COMMENT '设备指纹，防换机代签',
  PRIMARY KEY (checkin_id),
  UNIQUE KEY uk_checkin_reg (reg_id) COMMENT '一人一次签到',
  KEY idx_checkin_activity (activity_id),
  KEY idx_checkin_time (checkin_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='签到记录表';

-- ------------------------ 操作日志（A岗） ------------------------------
CREATE TABLE IF NOT EXISTS t_operation_log (
  log_id     BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  admin_id   BIGINT      DEFAULT NULL COMMENT '操作人',
  op_type    VARCHAR(40) DEFAULT NULL COMMENT '操作类型',
  target_id  VARCHAR(64) DEFAULT NULL COMMENT '操作对象ID',
  op_detail  VARCHAR(500) DEFAULT NULL COMMENT '操作详情',
  ip         VARCHAR(50) DEFAULT NULL COMMENT '来源IP',
  op_time    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (log_id),
  KEY idx_log_admin (admin_id),
  KEY idx_log_time (op_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- ============================ 初始化数据 ================================
INSERT INTO t_activity_type (type_code, type_name, sort_order, enabled) VALUES
  ('LECTURE',    '讲座',      1, 1),
  ('SEMINAR',    '宣讲会',    2, 1),
  ('CAREER',     '就业指导会',3, 1),
  ('TRAINING',   '培训',      4, 1),
  ('OTHER',      '其他',      9, 1);

INSERT INTO t_college (college_code, college_name, sort_order) VALUES
  ('CS',   '计算机科学与技术学院', 1),
  ('SE',   '软件学院',            2),
  ('EE',   '电子工程学院',        3),
  ('ME',   '机电工程学院',        4),
  ('BIZ',  '经济与管理学院',      5),
  ('ART',  '人文学院',            6),
  ('OTHER','其他',                99);

-- 默认超级管理员（admin / admin123）由后端 DataInitializer 在首次启动时
-- 检测 t_admin 为空后自动创建，密码以 BCrypt 加盐写入，脚本内不含明文口令。
CREATE TABLE IF NOT EXISTS t_activity_review (
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