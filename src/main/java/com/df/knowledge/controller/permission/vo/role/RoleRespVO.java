package com.df.knowledge.controller.permission.vo.role;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理后台 - 角色 Response VO
 */
@Data
@Schema(description = "管理后台 - 角色 Response VO")
public class RoleRespVO {

    @Schema(description = "角色编号", example = "1")
    private Long id;

    @Schema(description = "角色名称", example = "实例管理员")
    private String name;

    @Schema(description = "角色标识", example = "instance_admin")
    private String code;

    @Schema(description = "显示顺序", example = "1")
    private Integer sort;

    @Schema(description = "角色状态（0 开启 1 停用）", example = "0")
    private Integer status;

    @Schema(description = "角色类型（1 内置 2 自定义）", example = "1")
    private Integer type;

    @Schema(description = "备注", example = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
