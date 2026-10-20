package com.df.knowledge.enums;

import com.df.knowledge.common.ErrorCode;
import com.df.knowledge.common.ErrorCodeImpl;

/**
 * 业务错误码常量
 *
 * 编码为四段十位制 {@code 1-001-001-002}（类型-系统-模块-序号）：第一段固定 1（业务级异常），
 * 第二段固定 001，第三段按业务模块认领（001 用户功能：认证/用户/角色/菜单，002 知识库模块），
 * 第四段为模块内序号
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

    // ========== 知识库模块 1-001-002-0xx ==========
    ErrorCode KB_NOT_FOUND = new ErrorCodeImpl(1_001_002_001, "知识库不存在");
    ErrorCode KB_DEFAULT_NOT_DELETABLE = new ErrorCodeImpl(1_001_002_002, "默认知识库不允许删除");
    ErrorCode KB_NO_PERMISSION = new ErrorCodeImpl(1_001_002_003, "无权操作该知识库");
    ErrorCode KB_PERSONAL_NOT_ALLOW_MEMBER = new ErrorCodeImpl(1_001_002_004, "个人知识库不支持添加成员");
    ErrorCode KB_MEMBER_EXISTS = new ErrorCodeImpl(1_001_002_005, "该用户已是知识库成员");
    ErrorCode KB_MEMBER_NOT_FOUND = new ErrorCodeImpl(1_001_002_006, "知识库成员不存在");
}
