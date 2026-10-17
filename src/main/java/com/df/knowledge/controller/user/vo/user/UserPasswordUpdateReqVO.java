package com.df.knowledge.controller.user.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管理后台 - 个人密码修改 Request VO
 */
@Data
@Schema(description = "管理后台 - 个人密码修改 Request VO")
public class UserPasswordUpdateReqVO {

    @Schema(description = "旧密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "123456")
    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    @Schema(description = "新密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "654321")
    @NotBlank(message = "新密码不能为空")
    private String newPassword;
}
