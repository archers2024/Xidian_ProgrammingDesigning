package com.campus.activity.common;


/**
 * 统一响应体：{code, message, data}。
 * 错误码规则：0 成功；4xxxx 业务失败；5xxxx 系统异常。
 */

public class R<T> {

    private int code;
    private String message;
    private T data;

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.setCode(0);
        r.setMessage("success");
        r.setData(data);
        return r;
    }

    public static <T> R<T> fail(int code, String message) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMessage(message);
        return r;
    }

    /** 业务失败：40001 参数错误，40002 无权限，40003 资源不存在，40004 状态不允许 */
    public static <T> R<T> business(String message) {
        return fail(40001, message);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
