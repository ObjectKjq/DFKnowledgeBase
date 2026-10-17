package com.df.knowledge.controller.user.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 管理后台 - 用户修改 Request VO
 */
@Data
@Schema(description = "管理后台 - 用户修改 Request VO")
public class UserUpdateReqVO {

    @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "用户编号不能为空")
    private Long id;

    @Schema(description = "用户昵称", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @NotBlank(message = "用户昵称不能为空")
    private String nickname;

    @Schema(description = "头像地址", example = "")
    private String avatar;

    @Schema(description = "用户邮箱", example = "zhangsan@df.com")
    private String email;

    @Schema(description = "手机号码", example = "13800000000")
    private String mobile;

    @Schema(description = "账号状态（0 开启 1 停用）", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "账号状态不能为空")
    private Integer status;

    @Schema(description = "备注", example = "备注")
    private String remark;

    @Schema(description = "角色编号列表", example = "[2]")
    private List<Long> roleIds;
}
