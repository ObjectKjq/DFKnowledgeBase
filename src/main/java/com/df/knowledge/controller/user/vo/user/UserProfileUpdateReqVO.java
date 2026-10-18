package com.df.knowledge.controller.user.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管理后台 - 个人资料修改 Request VO
 */
@Data
@Schema(description = "管理后台 - 个人资料修改 Request VO")
public class UserProfileUpdateReqVO {

    @Schema(description = "用户昵称", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @NotBlank(message = "用户昵称不能为空")
    private String nickname;

    @Schema(description = "头像地址", example = "")
    private String avatar;

    @Schema(description = "用户邮箱", example = "zhangsan@df.com")
    private String email;

    @Schema(description = "手机号码", example = "13800000000")
    private String mobile;
}
