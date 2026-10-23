package com.df.knowledge.controller.mcp.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.*;

import lombok.Data;

import java.util.List;

/** 修改本人凭证工具开关。 */
@Data
public class McpCredentialToolsReqVO {
    /** 凭证编号。 */
    @NotNull
    @Positive
    @Schema(description = "本人凭证编号")
    private Long id;

    /** 允许工具。 */
    @NotNull
    @Size(max = 3)
    @Schema(description = "允许工具名称，可为空")
    private List<@NotBlank String> tools;
}
