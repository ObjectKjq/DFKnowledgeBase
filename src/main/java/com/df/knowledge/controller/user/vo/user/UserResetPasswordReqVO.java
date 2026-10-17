package com.df.knowledge.controller.user.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理后台 - 重置用户密码 Request VO
 */
@Data
@Schema(description = "管理后台 - 重置用户密码 Request VO")
public class UserResetPasswordReqVO {

    @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "用户编号不能为空")
    private Long id;

    @Schema(description = "新密码（明文，落库前 BCrypt 加密）", requiredMode = Schema.RequiredMode.REQUIRED, example = "123456")
    @NotBlank(message = "新密码不能为空")
    private String password;
}
