package com.df.knowledge.controller.permission.vo.role;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 管理后台 - 角色分配菜单 Request VO
 */
@Data
@Schema(description = "管理后台 - 角色分配菜单 Request VO")
public class RoleAssignMenuReqVO {

    @Schema(description = "角色编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "角色编号不能为空")
    private Long roleId;

    @Schema(description = "菜单编号列表（空列表表示清空该角色的菜单权限）", example = "[1,100,101]")
    private List<Long> menuIds;
}
