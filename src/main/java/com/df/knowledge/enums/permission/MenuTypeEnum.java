package com.df.knowledge.enums.permission;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 菜单类型枚举
 */
@Getter
@AllArgsConstructor
public enum MenuTypeEnum {

    DIR(1, "目录"), // 目录
    MENU(2, "菜单"), // 菜单
    BUTTON(3, "按钮"); // 按钮（权限点）

    /**
     * 类型
     */
    private final Integer code;

    /**
     * 类型名
     */
    private final String name;
}
