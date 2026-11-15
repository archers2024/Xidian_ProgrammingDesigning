package com.campus.activity.security;

/**
 * 当前登录管理员上下文（ThreadLocal）。
 */
public class AdminContext {

    private static final ThreadLocal<CurrentAdmin> LOCAL = new ThreadLocal<>();

    public static void set(CurrentAdmin admin) {
        LOCAL.set(admin);
    }

    public static CurrentAdmin get() {
        return LOCAL.get();
    }

    public static Long adminId() {
        CurrentAdmin a = LOCAL.get();
        return a == null ? null : a.getAdminId();
    }

    public static void clear() {
        LOCAL.remove();
    }

    public static class CurrentAdmin {
        private final Long adminId;
        private final String username;
        private final String roleCode;

        public CurrentAdmin(Long adminId, String username, String roleCode) {
            this.adminId = adminId;
            this.username = username;
            this.roleCode = roleCode;
        }

        public Long getAdminId() {
            return adminId;
        }

        public String getUsername() {
            return username;
        }

        public String getRoleCode() {
            return roleCode;
        }

        public boolean isSuper() {
            return "SUPER".equals(roleCode);
        }
    }
}
