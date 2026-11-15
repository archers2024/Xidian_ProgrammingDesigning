-- =====================================================================
-- 演示数据：1 个「进行中、已开启签到」的活动 + 2 条报名 + 1 条签到
-- 用途：clone 仓库后想立刻看列表、宣传页、报名名单、签到记录、统计图表。
-- 警告：会先删除 activity_id = 1 的旧业务数据，别在真实数据上跑。
-- 前置：已导入 db/init.sql 或 db/campus_activity_snapshot.sql，且后端至少启动过一次
--       （t_admin 里应有默认管理员，created_by 取 1）。
-- 导入：mysql --default-character-set=utf8mb4 -uroot -p < db/demo-data.sql
-- =====================================================================
SET NAMES utf8mb4;
USE campus_activity;

-- 按依赖顺序清理：签到 -> 报名 -> 附件 -> 活动
DELETE FROM t_checkin       WHERE activity_id = 1;
DELETE FROM t_registration  WHERE activity_id = 1;
DELETE FROM t_attachment    WHERE activity_id = 1;
DELETE FROM t_activity      WHERE activity_id = 1;

INSERT INTO t_activity (
  activity_id, activity_no, signup_guid, title, type_code, introduction, location, lng_lat,
  start_time, end_time, signup_deadline, quota, signed_count, status,
  checkin_enabled, checkin_secret, checkin_radius, live_url, cover_url, publish_time,
  created_by, create_time, update_time, deleted
) VALUES (
  1, 'ACT-DEMO-001', 'demo-signup-guid-0001', '校园开源技术分享会', 'LECTURE',
  '演示用活动：介绍前后端分离项目的开发、联调与部署流程。', '图书馆一层报告厅', '108.900000,34.200000',
  DATE_SUB(NOW(), INTERVAL 10 MINUTE), DATE_ADD(NOW(), INTERVAL 3 HOUR),
  DATE_SUB(NOW(), INTERVAL 30 MINUTE), 50, 2, 'ONGOING',
  1, 'demo-checkin-secret-2026', 500, NULL, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY),
  1, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW(), 0
);

INSERT INTO t_registration (
  reg_id, activity_id, name, phone, college_code, source_channel, signup_time, status, deleted
) VALUES
  (1, 1, '张三', '13800000001', 'CS', 'SCAN', DATE_SUB(NOW(), INTERVAL 2 HOUR), 'VALID', 0),
  (2, 1, '李四', '13800000002', 'SE', 'LINK', DATE_SUB(NOW(), INTERVAL 1 HOUR), 'VALID', 0);

INSERT INTO t_checkin (
  checkin_id, reg_id, activity_id, checkin_time, method, lng_lat, device_fingerprint
) VALUES
  (1, 1, 1, DATE_SUB(NOW(), INTERVAL 5 MINUTE), 'BASIC', NULL, 'demo-device-001');

-- 演示手机号均为虚构号段，不含任何真实个人信息。
-- 二维码地址：campus.mobile.base-url + /signup/demo-signup-guid-0001
-- 宣传页：/m/promo/demo-signup-guid-0001
-- 签到大屏：/m/board/1
