package com.df.knowledge.controller.permission.vo.menu;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理后台 - 菜单创建/修改 Request VO
 */
@Data
@Schema(description = "管理后台 - 菜单创建/修改 Request VO")
public class MenuSaveReqVO {

    @Schema(description = "菜单编号（修改时必填）", example = "101")
    private Long id;

    @Schema(description = "菜单名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "用户管理")
    @NotBlank(message = "菜单名称不能为空")
    private String name;

    @Schema(description = "权限标识（按钮类型必填，如 system:user:list）", example = "system:user:list")
    private String permission;

    @Schema(description = "菜单类型（1 目录 2 菜单 3 按钮）", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "菜单类型不能为空")
    private Integer type;

    @Schema(description = "显示顺序", example = "1")
    private Integer sort;

    @Schema(description = "父菜单编号（根为 0）", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "父菜单编号不能为空")
    private Long parentId;

    @Schema(description = "路由地址", example = "/system/user")
    private String path;

    @Schema(description = "菜单图标", example = "")
    private String icon;

    @Schema(description = "菜单状态（0 开启 1 停用）", example = "0")
    private Integer status;

    @Schema(description = "是否可见", example = "true")
    private Boolean visible;
}
