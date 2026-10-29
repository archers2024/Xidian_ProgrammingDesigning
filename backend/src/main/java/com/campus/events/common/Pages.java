package com.campus.events.common;

public final class Pages {

    private Pages() {
    }

    public static int page(Integer page) {
        if (page == null || page < 1) {
            return 1;
        }
        return Math.min(page, 1000);
    }

    public static int pageSize(Integer pageSize, int max) {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, max);
    }
}
