package com.df.knowledge.controller.mcp.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 凭证摘要，查询接口永不提供完整令牌或摘要。 */
@Data
public class McpCredentialRespVO {
    @Schema(description = "凭证编号")
    private Long id;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "仅供辨认的前缀")
    private String tokenPrefix;

    @Schema(description = "允许工具")
    private List<String> tools;

    @Schema(description = "过期时间")
    private LocalDateTime expiresAt;

    @Schema(description = "撤销时间")
    private LocalDateTime revokedAt;

    @Schema(description = "最后使用时间")
    private LocalDateTime lastUsedAt;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
