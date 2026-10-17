package com.df.knowledge.controller.user.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 管理后台 - 用户创建 Request VO
 */
@Data
@Schema(description = "管理后台 - 用户创建 Request VO")
public class UserSaveReqVO {

    @Schema(description = "用户账号", requiredMode = Schema.RequiredMode.REQUIRED, example = "zhangsan")
    @NotBlank(message = "用户账号不能为空")
    private String username;

    @Schema(description = "密码（明文，落库前 BCrypt 加密）", requiredMode = Schema.RequiredMode.REQUIRED, example = "123456")
    @NotBlank(message = "密码不能为空")
    private String password;

    @Schema(description = "用户昵称", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @NotBlank(message = "用户昵称不能为空")
    private String nickname;

    @Schema(description = "头像地址", example = "")
    private String avatar;

    @Schema(description = "用户邮箱", example = "zhangsan@df.com")
    private String email;

    @Schema(description = "手机号码", example = "13800000000")
    private String mobile;

    @Schema(description = "账号状态（0 开启 1 停用），不传默认开启", example = "0")
    private Integer status;

    @Schema(description = "备注", example = "备注")
    private String remark;

    @Schema(description = "角色编号列表", example = "[2]")
    private List<Long> roleIds;
}
