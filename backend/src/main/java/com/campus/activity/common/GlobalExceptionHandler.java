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
    public org.springframework.http.ResponseEntity<R<Void>> handleBusiness(BusinessException e) {
        int status=e.getCode()==40029?429:e.getCode()==40009?409:e.getCode()==40003?404:200;
        return org.springframework.http.ResponseEntity.status(status).body(R.fail(e.getCode(),e.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public R<Void> handleValid(Exception e) {
        String msg = e instanceof MethodArgumentNotValidException m
                ? m.getBindingResult().getFieldError() != null
                    ? m.getBindingResult().getFieldError().getDefaultMessage() : "参数校验失败"
                : "参数校验失败";
        return R.fail(40001, msg);
    }

    @ExceptionHandler({org.springframework.web.servlet.resource.NoResourceFoundException.class, org.springframework.web.servlet.NoHandlerFoundException.class})
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.NOT_FOUND)
    public R<Void> handleMissing(Exception e) { return R.fail(40004, "资源不存在"); }

    @ExceptionHandler(Exception.class)
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return R.fail(50000, "系统开小差了，请稍后重试");
    }

    @ExceptionHandler({IllegalArgumentException.class, ClassCastException.class, java.time.format.DateTimeParseException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.web.bind.MissingServletRequestParameterException.class,
            org.springframework.web.multipart.MultipartException.class})
    public R<Void> handleInput(Exception e) {
        return R.fail(40001, "请求参数缺失或格式不正确");
    }

    @ExceptionHandler(org.springframework.dao.DuplicateKeyException.class)
    public R<Void> handleDuplicate(Exception e) { return R.fail(40001, "数据已存在，请勿重复提交"); }
}
