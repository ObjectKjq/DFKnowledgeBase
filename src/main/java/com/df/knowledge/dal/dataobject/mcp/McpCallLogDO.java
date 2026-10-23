package com.df.knowledge.dal.dataobject.mcp;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

import java.time.LocalDateTime;

/** MCP 安全审计，不记录正文、问题或令牌。 */
@Data
@TableName("mcp_call_log")
public class McpCallLogDO {
    /** 日志编号。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 来源凭证，关联 {@link McpCredentialDO#getId()}，无效令牌时为空。 */
    private Long credentialId;

    /** 凭证创建者，关联 {@link com.df.knowledge.dal.dataobject.user.UserDO#getId()}，无法认证时为空。 */
    private Long userId;

    /** 调用时间。 */
    private LocalDateTime callTime;

    /** 白名单工具名，非法名称记作 unknown。 */
    private String toolName;

    /** 已确认有权的目标知识库编号 JSON。 */
    private String knowledgeBaseIds;

    /** 返回条数。 */
    private Integer resultCount;

    /** SUCCESS、FAILED 或 REJECTED。 */
    private String outcome;

    /** 固定原因码，不存异常消息。 */
    private String reasonCode;
}
