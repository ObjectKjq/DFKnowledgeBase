package com.df.knowledge.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 错误码实现
 */
@AllArgsConstructor
@Getter
public class ErrorCodeImpl implements ErrorCode {

    /**
     * 错误码
     */
    private final int code;

    /**
     * 错误提示
     */
    private final String msg;
}
