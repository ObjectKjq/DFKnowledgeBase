package com.df.knowledge.enums;

import com.df.knowledge.common.ErrorCode;
import com.df.knowledge.common.ErrorCodeImpl;

/**
 * 业务错误码常量
 *
 * 编码为四段十位制 {@code 1-001-001-002}（类型-系统-模块-序号）：第一段固定 1（业务级异常），
 * 第二段固定 001，第三段按业务模块认领（001 用户功能：认证/用户/角色/菜单，002 知识库模块，003 文档模块），
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

    // ========== 文档模块 1-001-003-0xx ==========
    ErrorCode DOC_NOT_FOUND = new ErrorCodeImpl(1_001_003_001, "文档不存在");
    ErrorCode DOC_FORMAT_NOT_SUPPORTED = new ErrorCodeImpl(1_001_003_002, "不支持的文件格式，仅支持 PDF、Word、PPT、Excel、TXT、Markdown");
    ErrorCode DOC_FILE_TOO_LARGE = new ErrorCodeImpl(1_001_003_003, "文件大小超过 50 MB 上限");
    ErrorCode DOC_RETRY_NOT_ALLOWED = new ErrorCodeImpl(1_001_003_004, "仅失败的文档支持重试");
    ErrorCode DOC_DELETE_NOT_ALLOWED = new ErrorCodeImpl(1_001_003_005, "文档已删除，无法重复删除");
    ErrorCode DOC_RESTORE_NOT_ALLOWED = new ErrorCodeImpl(1_001_003_006, "仅已删除的文档支持恢复");
    ErrorCode DOC_RESTORE_EXPIRED = new ErrorCodeImpl(1_001_003_007, "文档删除超过 30 天，无法恢复");
    ErrorCode DOC_FILE_STORE_FAILED = new ErrorCodeImpl(1_001_003_008, "文件存储失败");
    ErrorCode DOC_PARSE_FAILED = new ErrorCodeImpl(1_001_003_009, "文档解析失败");
    ErrorCode DOC_EMBEDDING_FAILED = new ErrorCodeImpl(1_001_003_010, "文档向量化失败");
    ErrorCode DOC_INDEX_FAILED = new ErrorCodeImpl(1_001_003_011, "文档索引处理失败");
}
