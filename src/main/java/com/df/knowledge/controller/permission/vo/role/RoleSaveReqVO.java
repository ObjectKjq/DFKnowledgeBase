package com.df.knowledge.controller.permission.vo.role;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管理后台 - 角色创建/修改 Request VO
 */
@Data
@Schema(description = "管理后台 - 角色创建/修改 Request VO")
public class RoleSaveReqVO {

    @Schema(description = "角色编号（修改时必填）", example = "1")
    private Long id;

    @Schema(description = "角色名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "管理员")
    @NotBlank(message = "角色名称不能为空")
    private String name;

    @Schema(description = "角色标识", requiredMode = Schema.RequiredMode.REQUIRED, example = "admin")
    @NotBlank(message = "角色标识不能为空")
    private String code;

    @Schema(description = "显示顺序", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer sort;

    @Schema(description = "角色状态（0 开启 1 停用）", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer status;

    @Schema(description = "备注", example = "备注")
    private String remark;
}
