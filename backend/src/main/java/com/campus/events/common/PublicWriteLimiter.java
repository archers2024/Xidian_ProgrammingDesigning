package com.campus.events.common;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 公开写接口的宽松限制。校园网经常共用出口，阈值故意留得比较高。
 */
@Component
public class PublicWriteLimiter {

    private static final long WINDOW_MILLIS = 60_000L;
    private static final int LIMIT = 60;

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public void acquire(String action, String ip) {
        String key = action + "|" + (ip == null ? "unknown" : ip);
        long now = System.currentTimeMillis();
        Window window = windows.compute(key, (ignored, existing) -> {
            if (existing == null || now - existing.startMillis > WINDOW_MILLIS) {
                return new Window(now, 1);
            }
            existing.count++;
            return existing;
        });
        if (window.count > LIMIT) {
            throw new ApiException(ErrorCode.TOO_MANY_REQUESTS);
        }
    }

    private static final class Window {
        private final long startMillis;
        private int count;

        private Window(long startMillis, int count) {
            this.startMillis = startMillis;
            this.count = count;
        }
    }
}
