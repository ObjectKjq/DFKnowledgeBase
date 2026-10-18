package com.df.knowledge.enums;

import com.df.knowledge.common.ErrorCode;
import com.df.knowledge.common.ErrorCodeImpl;

/**
 * 业务错误码常量
 *
 * 编码为四段十位制 {@code 1-001-001-002}（类型-系统-模块-序号）：第一段固定 1（业务级异常），
 * 第二段固定 001，第三段 001 由用户功能（认证/用户/角色/菜单）认领，第四段为模块内序号
 */
public interface ErrorCodeConstants {

    // ========== 用户模块 1-001-001-0xx ==========
    ErrorCode USER_NOT_FOUND = new ErrorCodeImpl(1_001_001_001, "用户不存在");
    ErrorCode USER_USERNAME_DUPLICATE = new ErrorCodeImpl(1_001_001_002, "账号【{}】已存在");
    ErrorCode USER_PASSWORD_ERROR = new ErrorCodeImpl(1_001_001_003, "账号或密码错误");
    ErrorCode USER_IS_DISABLED = new ErrorCodeImpl(1_001_001_004, "账号已停用");
    ErrorCode USER_OLD_PASSWORD_ERROR = new ErrorCodeImpl(1_001_001_005, "旧密码不正确");

    // ========== 角色模块 1-001-001-1xx ==========
    ErrorCode ROLE_NOT_FOUND = new ErrorCodeImpl(1_001_001_101, "角色不存在");
    ErrorCode ROLE_NAME_DUPLICATE = new ErrorCodeImpl(1_001_001_102, "已经存在该名字的角色");
    ErrorCode ROLE_CODE_DUPLICATE = new ErrorCodeImpl(1_001_001_103, "已经存在该标识的角色");
    ErrorCode ROLE_SYSTEM_TYPE_PROHIBITED = new ErrorCodeImpl(1_001_001_104, "内置角色不允许删除");
    ErrorCode ROLE_HAS_USER = new ErrorCodeImpl(1_001_001_105, "该角色已分配给用户，无法删除");

    // ========== 菜单模块 1-001-001-2xx ==========
    ErrorCode MENU_NOT_FOUND = new ErrorCodeImpl(1_001_001_201, "菜单不存在");
    ErrorCode MENU_PARENT_ERROR = new ErrorCodeImpl(1_001_001_202, "父菜单不能是自己");
    ErrorCode MENU_PARENT_NOT_FOUND = new ErrorCodeImpl(1_001_001_203, "父菜单不存在");
    ErrorCode MENU_EXISTS_CHILD = new ErrorCodeImpl(1_001_001_204, "存在子菜单，无法删除");
}
