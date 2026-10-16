package com.df.knowledge.common.exception;

import cn.hutool.core.util.StrUtil;
import com.df.knowledge.common.CommonResult;
import com.df.knowledge.common.GlobalErrorCodeConstants;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Iterator;

/**
 * 全局异常处理器，将异常统一转换为 {@link CommonResult}
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 业务异常
     */
    @ExceptionHandler(ServiceException.class)
    public CommonResult<?> handleServiceException(ServiceException ex) {
        log.warn("[handleServiceException]", ex);
        return CommonResult.error(ex.getErrorCode());
    }

    /**
     * 参数绑定/校验异常（含 @RequestBody 的 MethodArgumentNotValidException）
     */
    @ExceptionHandler(BindException.class)
    public CommonResult<?> handleBindException(BindException ex) {
        log.warn("[handleBindException]", ex);
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String message = fieldError != null
                ? StrUtil.format("请求参数不正确：{} {}", fieldError.getField(), fieldError.getDefaultMessage())
                : GlobalErrorCodeConstants.BAD_REQUEST.getMsg();
        return CommonResult.error(GlobalErrorCodeConstants.BAD_REQUEST.getCode(), message);
    }

    /**
     * 单个参数校验异常（@PathVariable、@RequestParam 等）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public CommonResult<?> handleConstraintViolationException(ConstraintViolationException ex) {
        log.warn("[handleConstraintViolationException]", ex);
        Iterator<ConstraintViolation<?>> iterator = ex.getConstraintViolations().iterator();
        String message = GlobalErrorCodeConstants.BAD_REQUEST.getMsg();
        if (iterator.hasNext()) {
            ConstraintViolation<?> violation = iterator.next();
            message = StrUtil.format("请求参数不正确：{} {}", violation.getPropertyPath(), violation.getMessage());
        }
        return CommonResult.error(GlobalErrorCodeConstants.BAD_REQUEST.getCode(), message);
    }

    /**
     * 兜底异常
     */
    @ExceptionHandler(Throwable.class)
    public CommonResult<?> handleThrowable(Throwable ex) {
        log.error("[handleThrowable]", ex);
        return CommonResult.error(GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR);
    }
}
