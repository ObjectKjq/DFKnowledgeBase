package com.df.knowledge.controller.auth.vo.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管理后台 - 登录 Request VO
 */
@Data
@Schema(description = "管理后台 - 登录 Request VO")
public class AuthLoginReqVO {

    @Schema(description = "用户账号", requiredMode = Schema.RequiredMode.REQUIRED, example = "admin")
    @NotBlank(message = "用户账号不能为空")
    private String username;

    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "admin123")
    @NotBlank(message = "密码不能为空")
    private String password;
}
