package com.df.knowledge.controller.auth.vo.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 管理后台 - 登录 Response VO
 */
@Data
@Schema(description = "管理后台 - 登录 Response VO")
public class AuthLoginRespVO {

    @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long userId;

    @Schema(description = "访问令牌", requiredMode = Schema.RequiredMode.REQUIRED, example = "eab8a5c7-8f3a-4b6e-9c1d-2f3a4b6e9c1d")
    private String token;
}
