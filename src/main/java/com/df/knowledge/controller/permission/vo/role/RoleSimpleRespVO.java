package com.df.knowledge.controller.permission.vo.role;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 管理后台 - 角色精简 Response VO
 */
@Data
@Schema(description = "管理后台 - 角色精简 Response VO")
public class RoleSimpleRespVO {

    @Schema(description = "角色编号", example = "1")
    private Long id;

    @Schema(description = "角色名称", example = "管理员")
    private String name;
}
