package com.campus.events.common;

public class ApiException extends RuntimeException {

    private final int status;
    private final String code;

    public ApiException(ErrorCode error) {
        this(error, error.defaultMessage());
    }

    public ApiException(ErrorCode error, String message) {
        super(message);
        this.status = error.status();
        this.code = error.name();
    }

    public ApiException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public int getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
