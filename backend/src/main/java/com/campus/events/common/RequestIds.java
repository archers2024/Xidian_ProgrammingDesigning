package com.campus.events.common;

public final class RequestIds {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private RequestIds() {
    }

    public static void set(String id) {
        CURRENT.set(id);
    }

    public static String current() {
        String id = CURRENT.get();
        return id == null ? "" : id;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
