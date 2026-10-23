package com.df.knowledge.dal.dataobject.mcp;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

import java.time.LocalDateTime;

/** 用户创建的 MCP 凭证，仅持久化令牌摘要。 */
@Data
@TableName("mcp_credential")
public class McpCredentialDO {
    /** 凭证编号。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 创建者编号，关联 {@link com.df.knowledge.dal.dataobject.user.UserDO#getId()}。 */
    private Long userId;

    /** 凭证名称。 */
    private String name;

    /** SHA-256 摘要，不返回客户端。 */
    private String tokenHash;

    /** 无法用于认证的令牌前缀。 */
    private String tokenPrefix;

    /** 允许工具，逗号分隔白名单名称。 */
    private String tools;

    /** 过期时间，等于当前时间时已失效。 */
    private LocalDateTime expiresAt;

    /** 撤销时间。 */
    private LocalDateTime revokedAt;

    /** 最近成功认证时间。 */
    private LocalDateTime lastUsedAt;

    /** 创建时间。 */
    private LocalDateTime createTime;

    /** 限速窗口起点，UTC 分钟编号。 */
    private Long rateWindowStart;

    /** 窗口内调用计数。 */
    private Integer rateWindowCount;
}
