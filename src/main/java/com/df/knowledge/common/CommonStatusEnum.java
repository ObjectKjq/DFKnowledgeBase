package com.df.knowledge.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通用状态枚举
 */
@Getter
@AllArgsConstructor
public enum CommonStatusEnum {

    ENABLE(0, "开启"), // 开启
    DISABLE(1, "停用"); // 停用

    /**
     * 状态
     */
    private final Integer code;

    /**
     * 状态名
     */
    private final String name;
}
