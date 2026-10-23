package com.df.knowledge.controller.mcp.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

import java.time.LocalDateTime;

/** 本人脱敏调用记录。 */
@Data
public class McpCallLogRespVO {
    @Schema(description = "记录编号")
    private Long id;

    @Schema(description = "来源凭证编号")
    private Long credentialId;

    @Schema(description = "调用时间")
    private LocalDateTime callTime;

    @Schema(description = "工具名称")
    private String toolName;

    @Schema(description = "已确认有权的知识库编号 JSON")
    private String knowledgeBaseIds;

    @Schema(description = "结果条数")
    private Integer resultCount;

    @Schema(description = "SUCCESS、FAILED 或 REJECTED")
    private String outcome;

    @Schema(description = "固定原因码")
    private String reasonCode;
}
