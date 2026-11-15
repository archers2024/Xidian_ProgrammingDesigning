package com.campus.activity.common;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理：业务异常返回可执行提示，系统异常统一兜底，绝不把堆栈抛给前端（NFR-U3）。
 */

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);


    @ExceptionHandler(BusinessException.class)
    public R<Void> handleBusiness(BusinessException e) {
        return R.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public R<Void> handleValid(Exception e) {
        String msg = e instanceof MethodArgumentNotValidException m
                ? m.getBindingResult().getFieldError() != null
                    ? m.getBindingResult().getFieldError().getDefaultMessage() : "参数校验失败"
                : "参数校验失败";
        return R.fail(40001, msg);
    }

    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return R.fail(50000, "系统开小差了，请稍后重试");
    }
}
