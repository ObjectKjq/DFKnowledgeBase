package com.df.knowledge.common.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
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
import org.springframework.web.servlet.resource.NoResourceFoundException;

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
     * sa-token 未登录异常（未登录、令牌失效、被踢下线）
     */
    @ExceptionHandler(NotLoginException.class)
    public CommonResult<?> handleNotLoginException(NotLoginException ex) {
        log.warn("[handleNotLoginException][{}]", ex.getType());
        return CommonResult.error(GlobalErrorCodeConstants.UNAUTHORIZED);
    }

    /**
     * sa-token 无权限异常（权限串不满足、角色不满足）
     */
    @ExceptionHandler({NotPermissionException.class, NotRoleException.class})
    public CommonResult<?> handleNotPermissionException(Exception ex) {
        log.warn("[handleNotPermissionException]", ex);
        return CommonResult.error(GlobalErrorCodeConstants.FORBIDDEN);
    }

    /**
     * 静态资源或接口路径不存在（例如访问未映射的 URL、缺失的文档页面）
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public CommonResult<?> handleNoResourceFoundException(NoResourceFoundException ex) {
        log.warn("[handleNoResourceFoundException][{}]", ex.getMessage());
        return CommonResult.error(GlobalErrorCodeConstants.NOT_FOUND);
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
