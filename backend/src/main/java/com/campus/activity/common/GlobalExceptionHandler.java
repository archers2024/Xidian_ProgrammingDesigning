package com.campus.activity.common;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.format.DateTimeParseException;

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

    /**
     * 参数形态错误统一归为 40001，不再落入 50000。
     * 覆盖：路径/查询参数类型不匹配、缺少必填参数、请求体不是合法 JSON、字段格式解析失败。
     */
    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class,
            DateTimeParseException.class,
            NumberFormatException.class
    })
    public R<Void> handleBadRequest(Exception e) {
        log.warn("请求参数错误：{}", e.getMessage());
        return R.fail(40001, "请求参数格式不正确，请检查后重试");
    }

    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return R.fail(50000, "系统开小差了，请稍后重试");
    }
}
