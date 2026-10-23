package com.df.knowledge.enums;

import com.df.knowledge.common.ErrorCode;
import com.df.knowledge.common.ErrorCodeImpl;

/**
 * 业务错误码常量
 *
 * 编码为四段十位制 {@code 1-001-001-002}（类型-系统-模块-序号）：第一段固定 1（业务级异常），
 * 第二段固定 001，第三段按业务模块认领（001 用户功能：认证/用户/角色/菜单，002 知识库模块，003 文档模块，004 会话模块，005 文件模块），
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

    ErrorCode KB_RESTORE_NOT_ALLOWED = new ErrorCodeImpl(1_001_002_007, "知识库未删除，无法恢复或彻底删除");
    ErrorCode KB_RESTORE_EXPIRED = new ErrorCodeImpl(1_001_002_008, "知识库删除超过 30 天，无法恢复");
    ErrorCode KB_PURGE_CONFIRM_REQUIRED = new ErrorCodeImpl(1_001_002_009, "请确认彻底删除知识库");

    // ========== 文档模块 1-001-003-0xx ==========
    ErrorCode DOC_NOT_FOUND = new ErrorCodeImpl(1_001_003_001, "文档不存在");
    ErrorCode DOC_FORMAT_NOT_SUPPORTED = new ErrorCodeImpl(1_001_003_002, "不支持的文件格式，仅支持 PDF、Word、PPT、Excel、TXT、Markdown");
    ErrorCode DOC_RETRY_NOT_ALLOWED = new ErrorCodeImpl(1_001_003_004, "仅失败的文档支持重试");
    ErrorCode DOC_DELETE_NOT_ALLOWED = new ErrorCodeImpl(1_001_003_005, "文档已删除，无法重复删除");
    ErrorCode DOC_RESTORE_NOT_ALLOWED = new ErrorCodeImpl(1_001_003_006, "仅已删除的文档支持恢复");
    ErrorCode DOC_RESTORE_EXPIRED = new ErrorCodeImpl(1_001_003_007, "文档删除超过 30 天，无法恢复");
    ErrorCode DOC_PARSE_FAILED = new ErrorCodeImpl(1_001_003_009, "文档解析失败");
    ErrorCode DOC_EMBEDDING_FAILED = new ErrorCodeImpl(1_001_003_010, "文档向量化失败");
    ErrorCode DOC_INDEX_FAILED = new ErrorCodeImpl(1_001_003_011, "文档索引处理失败");
    ErrorCode DOC_SEARCH_QUERY_EMPTY = new ErrorCodeImpl(1_001_003_012, "检索内容不能为空");
    ErrorCode DOC_SEARCH_FAILED = new ErrorCodeImpl(1_001_003_013, "文档检索失败");

    ErrorCode DOC_PURGE_NOT_ALLOWED = new ErrorCodeImpl(1_001_003_014, "仅回收站文档支持彻底删除");
    ErrorCode DOC_PURGE_CONFIRM_REQUIRED = new ErrorCodeImpl(1_001_003_015, "请确认彻底删除文档");

    // ========== 会话模块 1-001-004-0xx ==========
    ErrorCode CONVERSATION_NOT_FOUND = new ErrorCodeImpl(1_001_004_001, "会话不存在");
    ErrorCode RAG_MODEL_UNAVAILABLE = new ErrorCodeImpl(1_001_004_002, "对话模型不可用，请联系管理员检查模型配置后重试");
    ErrorCode RAG_GENERATION_TIMEOUT = new ErrorCodeImpl(1_001_004_003, "答案生成超时，请重试");

    // ========== 文件模块 1-001-005-0xx ==========
    ErrorCode FILE_NOT_FOUND = new ErrorCodeImpl(1_001_005_001, "文件不存在");
    ErrorCode FILE_IS_EMPTY = new ErrorCodeImpl(1_001_005_002, "文件为空");
    ErrorCode FILE_TYPE_NOT_ALLOWED = new ErrorCodeImpl(1_001_005_003, "不支持的文件类型");
    ErrorCode FILE_TOO_LARGE = new ErrorCodeImpl(1_001_005_004, "文件大小超过 50 MB 上限");
    ErrorCode FILE_STORE_FAILED = new ErrorCodeImpl(1_001_005_005, "文件存储失败");
    ErrorCode FILE_IN_USE = new ErrorCodeImpl(1_001_005_006, "文件仍被文档引用，请从文档回收站彻底删除");

    // ========== 模型与实例设置模块 1-001-010-0xx ==========
    ErrorCode MODEL_NOT_FOUND = new ErrorCodeImpl(1_001_010_001, "模型配置不存在");
    ErrorCode MODEL_NAME_DUPLICATE = new ErrorCodeImpl(1_001_010_002, "已经存在该名字的模型配置");
    ErrorCode MODEL_NOT_CONFIGURED = new ErrorCodeImpl(1_001_010_003, "未配置对话模型，请联系管理员");
    ErrorCode MODEL_CONNECTION_FAILED = new ErrorCodeImpl(1_001_010_004, "模型服务不可达，请联系管理员");
    ErrorCode MODEL_AUTH_FAILED = new ErrorCodeImpl(1_001_010_005, "模型服务鉴权失败，请联系管理员");
    ErrorCode MODEL_TIMEOUT = new ErrorCodeImpl(1_001_010_006, "模型调用超时，请重试");
    ErrorCode MODEL_RESPONSE_INVALID = new ErrorCodeImpl(1_001_010_007, "模型响应无效，请联系管理员");
    ErrorCode MODEL_ADDRESS_INVALID = new ErrorCodeImpl(1_001_010_008, "模型地址必须是无凭据及查询参数的 HTTP 或 HTTPS 地址");
    ErrorCode MODEL_RERANK_DUPLICATE = new ErrorCodeImpl(1_001_010_009, "仅允许配置一个重排模型");
    // ========== MCP 接入模块 1-001-011-0xx ==========
    ErrorCode MCP_CREDENTIAL_NOT_FOUND = new ErrorCodeImpl(1_001_011_001, "MCP 凭证不存在");
    ErrorCode MCP_CREDENTIAL_INVALID = new ErrorCodeImpl(1_001_011_002, "MCP 凭证配置无效");
}
