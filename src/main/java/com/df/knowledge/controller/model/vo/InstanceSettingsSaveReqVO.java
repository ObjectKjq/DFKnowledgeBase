package com.df.knowledge.controller.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

/** 实例运行设置。 */
@Data
public class InstanceSettingsSaveReqVO {
    @Schema(description = "MCP 总开关")
    @NotNull
    private Boolean mcpEnabled;

    @Schema(description = "每凭证每分钟工具调用上限，默认 60")
    @NotNull
    @Min(1)
    @Max(100000)
    private Integer mcpRateLimitPerMinute;

    @Schema(description = "全实例入库并发，默认 2")
    @NotNull
    @Min(1)
    @Max(64)
    private Integer ingestGlobalConcurrency;

    @Schema(description = "单知识库入库并发，默认 1")
    @NotNull
    @Min(1)
    @Max(64)
    private Integer ingestKnowledgeBaseConcurrency;
}
