package com.campus.activity.util;

import com.campus.activity.common.BusinessException;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

/** Short synchronized operations; password hashing never holds this monitor. */
public final class LoginAttemptLimiter {
    private record Attempt(int failures, long expires) {}
    private final Map<String, Attempt> attempts = new HashMap<>();
    private final Clock clock;
    private static final long WINDOW = 30 * 60 * 1000L;

    public LoginAttemptLimiter() { this(Clock.systemUTC()); }
    public LoginAttemptLimiter(Clock clock) { this.clock = clock; }

    public synchronized void check(String username) {
        long now = clock.millis();
        attempts.entrySet().removeIf(e -> e.getValue().expires() <= now);
        Attempt a = attempts.get(username);
        if (a != null && a.failures() >= 5) throw new BusinessException("账号已锁定，请在 30 分钟后重试或联系管理员重置密码");
    }

    public synchronized void failure(String username) {
        check(username);
        Attempt old = attempts.get(username);
        int count = old == null ? 1 : old.failures() + 1;
        attempts.put(username, new Attempt(count, clock.millis() + WINDOW));
        if (count >= 5) throw new BusinessException("密码错误次数过多，账号已锁定 30 分钟");
    }

    public synchronized void clear(String username) { attempts.remove(username); }
}
