package com.df.knowledge.enums.permission;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 角色类型枚举
 */
@Getter
@AllArgsConstructor
public enum RoleTypeEnum {

    SYSTEM(1, "内置"), // 内置角色
    CUSTOM(2, "自定义"); // 自定义角色

    /**
     * 类型
     */
    private final Integer code;

    /**
     * 类型名
     */
    private final String name;
}
