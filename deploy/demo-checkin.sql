-- 演示前执行：报名完成后开启签到，并把活动推进到「进行中」
-- 用法：mysql campus_activity < demo-checkin.sql
-- 注意：把 @aid 改成详情页 URL 里的实际活动 ID（默认 1）。

USE campus_activity;
SET @aid = 1;

-- 为什么不直接改 status 了事：
-- ActivityService.refreshStatus 每 60 秒按时间字段重算状态，手工改 status 会被改回去。
-- 所以把 signup_deadline / start_time 移到过去、end_time 放到未来，
-- 让状态机自己算出 ONGOING；刷新详情页也会立即触发 refreshStatus。
UPDATE t_activity
SET checkin_enabled = 1,
    signup_deadline = DATE_SUB(NOW(), INTERVAL 30 MINUTE),
    start_time      = DATE_SUB(NOW(), INTERVAL 10 MINUTE),
    end_time        = DATE_ADD(NOW(), INTERVAL 3 HOUR),
    status          = 'ONGOING'
WHERE activity_id = @aid AND deleted = 0;

SELECT activity_id, title, status, checkin_enabled,
       signup_deadline, start_time, end_time
FROM t_activity
WHERE activity_id = @aid;

-- 说明：checkin_enabled 在建活动时写死为 0，且没有任何接口能打开它，
-- 只能从数据库改。此 UPDATE 不会被定时任务覆盖。
--
-- 演示顺序必须是「先报名，后执行本脚本」：
--   报名接口只放行 OPEN（now < signup_deadline），
--   签到接口只放行 ONGOING / CLOSED，两者天然互斥。
