package com.df.knowledge.controller.permission.vo.role;

import com.df.knowledge.common.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 管理后台 - 角色分页查询 Request VO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "管理后台 - 角色分页查询 Request VO")
public class RolePageReqVO extends PageParam {

    @Schema(description = "角色名称（模糊匹配）", example = "管理")
    private String name;

    @Schema(description = "角色标识（模糊匹配）", example = "admin")
    private String code;

    @Schema(description = "角色状态（0 开启 1 停用）", example = "0")
    private Integer status;
}
