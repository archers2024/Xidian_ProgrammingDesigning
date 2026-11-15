package com.campus.activity.common;

/**
 * 业务异常：由各业务域抛出，经全局异常处理器转成统一响应，不打印堆栈到前端。
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = 40001;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
