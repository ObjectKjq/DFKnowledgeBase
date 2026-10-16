package com.df.knowledge.common.exception;

import com.df.knowledge.common.ErrorCode;

/**
 * 业务异常
 */
public class ServiceException extends RuntimeException {

    /**
     * 错误码
     */
    private final ErrorCode errorCode;

    public ServiceException(ErrorCode errorCode) {
        super(errorCode.getMsg());
        this.errorCode = errorCode;
    }

    /**
     * 使用自定义提示覆盖错误码默认提示
     */
    public ServiceException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
