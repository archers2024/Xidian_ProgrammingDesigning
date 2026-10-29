package com.campus.events.common;

public enum ErrorCode {
    VALIDATION_ERROR(400, "提交的数据不符合要求"),
    INVALID_CREDENTIALS(401, "账号或密码不正确"),
    UNAUTHENTICATED(401, "登录状态已失效，请重新登录"),
    CSRF_INVALID(403, "页面已过期，请刷新后重试"),
    FORBIDDEN(403, "没有权限执行此操作"),
    ACTIVITY_NOT_FOUND(404, "活动不存在或已被删除"),
    ACTIVITY_UNAVAILABLE(404, "活动不存在或尚未发布"),
    ATTACHMENT_NOT_FOUND(404, "附件不存在"),
    NOT_REGISTERED(404, "该手机号未报名此活动"),
    SESSION_NOT_FOUND(404, "签到会话不存在"),
    VERSION_CONFLICT(409, "活动已被其他管理员修改，请重新读取后再保存"),
    REGISTRATION_DUPLICATE(409, "该手机号已报名本活动"),
    REGISTRATION_FULL(409, "活动名额已满"),
    REGISTRATION_CLOSED(409, "报名已截止"),
    CHECKIN_CLOSED(409, "签到尚未开启或已关闭"),
    CHECKIN_NOT_OPEN(409, "当前不在签到时间内"),
    CHECKIN_TOKEN_EXPIRED(409, "签到码已过期，请重新扫描会场二维码"),
    CHECKIN_TOKEN_INVALID(400, "签到码无效，请重新扫描会场二维码"),
    CHECKIN_NOT_DYNAMIC(409, "当前签到不是动态二维码模式"),
    TOO_MANY_REQUESTS(429, "操作过于频繁，请稍后再试"),
    INTERNAL_ERROR(500, "服务暂时不可用，请稍后重试");

    private final int status;
    private final String defaultMessage;

    ErrorCode(int status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public int status() {
        return status;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
