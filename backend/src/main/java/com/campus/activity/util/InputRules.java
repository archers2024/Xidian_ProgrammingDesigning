package com.campus.activity.util;

import com.campus.activity.common.BusinessException;
import java.net.URI;

public final class InputRules {
    private InputRules() {}

    public static void text(String value, int max, boolean required, String label) {
        if ((required && (value == null || value.isBlank())) || (value != null && value.length() > max)) {
            throw new BusinessException(label + "不能为空或超过 " + max + " 个字符");
        }
    }

    public static void publicUrl(String value, String label) {
        text(value, 500, false, label);
        if (value == null || value.isBlank()) return;
        try {
            URI uri = new URI(value);
            boolean local = value.startsWith("/") && !value.startsWith("//") && !value.contains("\\");
            boolean web = ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null && uri.getUserInfo() == null;
            if (!local && !web) throw new IllegalArgumentException();
        } catch (Exception e) {
            throw new BusinessException(label + "仅支持站内路径或 HTTP/HTTPS 地址");
        }
    }
}
