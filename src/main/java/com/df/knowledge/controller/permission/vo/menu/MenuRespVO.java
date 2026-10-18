package com.df.knowledge.controller.permission.vo.menu;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理后台 - 菜单 Response VO
 */
@Data
@Schema(description = "管理后台 - 菜单 Response VO")
public class MenuRespVO {

    @Schema(description = "菜单编号", example = "1")
    private Long id;

    @Schema(description = "菜单名称", example = "用户管理")
    private String name;

    @Schema(description = "权限标识", example = "system:user:list")
    private String permission;

    @Schema(description = "菜单类型（1 目录 2 菜单 3 按钮）", example = "2")
    private Integer type;

    @Schema(description = "显示顺序", example = "1")
    private Integer sort;

    @Schema(description = "父菜单ID（根为 0）", example = "0")
    private Long parentId;

    @Schema(description = "路由地址", example = "/system/user")
    private String path;

    @Schema(description = "菜单图标", example = "")
    private String icon;

    @Schema(description = "菜单状态（0 开启 1 停用）", example = "0")
    private Integer status;

    @Schema(description = "是否可见", example = "true")
    private Boolean visible;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
