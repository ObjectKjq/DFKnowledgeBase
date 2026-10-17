package com.df.knowledge.enums.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 登录结果枚举
 */
@Getter
@AllArgsConstructor
public enum LoginResultEnum {

    SUCCESS(0, "成功"), // 登录成功
    BAD_CREDENTIALS(10, "账号或密码错"), // 账号不存在或密码不匹配
    USER_DISABLED(20, "用户被禁用"); // 账号处于停用状态

    /**
     * 结果
     */
    private final Integer code;

    /**
     * 结果名
     */
    private final String name;
}
