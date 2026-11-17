package com.campus.activity.security;

import com.campus.activity.common.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;

/** Database-backed atomic buckets are shared across nodes and survive process restarts. */
@Service
public class RateLimitStore {
    private final JdbcTemplate jdbc;
    public RateLimitStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public int increment(String key, int seconds) {
        jdbc.update("INSERT INTO t_rate_limit(bucket_key,hits,expires_at) VALUES (?,1,TIMESTAMPADD(SECOND,?,NOW())) ON DUPLICATE KEY UPDATE hits=IF(expires_at<=NOW(),1,hits+1),expires_at=IF(expires_at<=NOW(),VALUES(expires_at),expires_at)", key, seconds);
        return jdbc.queryForObject("SELECT hits FROM t_rate_limit WHERE bucket_key=?", Integer.class, key);
    }

    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void take(String key, int limit, int seconds) {
        if (increment(key, seconds) > limit) throw new BusinessException(40029,"操作过于频繁，请稍后再试");
    }
    public void requireBelow(String key, int limit) {
        Integer count = jdbc.queryForObject("SELECT COALESCE(MAX(IF(expires_at>NOW(),hits,0)),0) FROM t_rate_limit WHERE bucket_key=?", Integer.class, key);
        if (count != null && count >= limit) throw new BusinessException(40029,"账号已锁定，请稍后重试或联系管理员重置密码");
    }
    public void clear(String key) { jdbc.update("DELETE FROM t_rate_limit WHERE bucket_key=?",key); }
    @Scheduled(fixedDelay=60000) public void cleanup() {
        jdbc.update("DELETE FROM t_rate_limit WHERE expires_at<NOW() LIMIT 1000");
        jdbc.update("DELETE FROM t_idempotency WHERE expires_at<NOW() LIMIT 1000");
    }
}
