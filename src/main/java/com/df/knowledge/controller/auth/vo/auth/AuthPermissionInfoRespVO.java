package com.df.knowledge.controller.auth.vo.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 管理后台 - 登录用户的权限信息 Response VO
 */
@Data
@Schema(description = "管理后台 - 登录用户的权限信息 Response VO")
public class AuthPermissionInfoRespVO {

    @Schema(description = "用户信息")
    private UserVO user;

    @Schema(description = "角色标识列表", example = "[\"instance_admin\"]")
    private List<String> roles;

    @Schema(description = "权限标识列表", example = "[\"system:user:list\"]")
    private List<String> permissions;

    @Schema(description = "菜单列表（树形，不含按钮）")
    private List<MenuVO> menus;

    /**
     * 用户信息 VO
     */
    @Data
    @Schema(description = "管理后台 - 登录用户的用户信息 Response VO")
    public static class UserVO {

        @Schema(description = "用户编号", example = "1")
        private Long id;

        @Schema(description = "用户账号", example = "admin")
        private String username;

        @Schema(description = "用户昵称", example = "管理员")
        private String nickname;

        @Schema(description = "头像地址", example = "")
        private String avatar;
    }

    /**
     * 菜单信息 VO（树形）
     */
    @Data
    @Schema(description = "管理后台 - 登录用户的菜单 Response VO")
    public static class MenuVO {

        @Schema(description = "菜单编号", example = "100")
        private Long id;

        @Schema(description = "菜单名称", example = "系统管理")
        private String name;

        @Schema(description = "权限标识", example = "")
        private String permission;

        @Schema(description = "菜单类型（1 目录 2 菜单）", example = "1")
        private Integer type;

        @Schema(description = "显示顺序", example = "1")
        private Integer sort;

        @Schema(description = "父菜单ID（根为 0）", example = "0")
        private Long parentId;

        @Schema(description = "路由地址", example = "/system")
        private String path;

        @Schema(description = "菜单图标", example = "")
        private String icon;

        @Schema(description = "是否可见", example = "true")
        private Boolean visible;

        @Schema(description = "子菜单")
        private List<MenuVO> children;
    }
}
