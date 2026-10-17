package com.df.knowledge.controller.user.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理后台 - 修改用户状态 Request VO
 */
@Data
@Schema(description = "管理后台 - 修改用户状态 Request VO")
public class UserUpdateStatusReqVO {

    @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "用户编号不能为空")
    private Long id;

    @Schema(description = "账号状态（0 开启 1 停用）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "账号状态不能为空")
    private Integer status;
}
