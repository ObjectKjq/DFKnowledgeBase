package com.df.knowledge.controller.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** 实例运行设置读取结果。 */
@Data
public class InstanceSettingsRespVO {

    @Schema(description = "MCP 总开关")
    private Boolean mcpEnabled;

    @Schema(description = "每凭证每分钟工具调用上限")
    private Integer mcpRateLimitPerMinute;

    @Schema(description = "全实例入库并发")
    private Integer ingestGlobalConcurrency;

    @Schema(description = "单知识库入库并发")
    private Integer ingestKnowledgeBaseConcurrency;
}
