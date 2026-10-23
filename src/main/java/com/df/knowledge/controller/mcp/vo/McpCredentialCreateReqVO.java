package com.df.knowledge.controller.mcp.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.*;

import lombok.Data;

import java.util.List;

/** 创建个人 MCP 凭证。 */
@Data
public class McpCredentialCreateReqVO {
    /** 凭证名称。 */
    @NotBlank
    @Size(max = 80)
    @Schema(description = "凭证名称")
    private String name;

    /** 独立工具许可。 */
    @NotNull
    @Size(max = 3)
    @Schema(description = "允许工具：search、ask、list_knowledge_bases，可为空")
    private List<@NotBlank String> tools;

    /** 有效期，无永久凭证。 */
    @NotNull
    @Min(1)
    @Max(365)
    @Schema(description = "有效天数，默认 90")
    private Integer validDays = 90;
}
