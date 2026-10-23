package com.df.knowledge.controller.mcp.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

/** 创建结果，完整令牌仅展示一次。 */
@Data
public class McpCredentialCreatedRespVO extends McpCredentialRespVO {
    @Schema(description = "仅本次返回的完整令牌")
    private String token;

    @Schema(description = "接入指引")
    private String instructions;
}
