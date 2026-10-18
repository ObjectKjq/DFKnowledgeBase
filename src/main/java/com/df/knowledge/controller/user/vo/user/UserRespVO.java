package com.df.knowledge.controller.user.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理后台 - 用户 Response VO
 */
@Data
@Schema(description = "管理后台 - 用户 Response VO")
public class UserRespVO {

    @Schema(description = "用户编号", example = "1")
    private Long id;

    @Schema(description = "用户账号", example = "admin")
    private String username;

    @Schema(description = "用户昵称", example = "管理员")
    private String nickname;

    @Schema(description = "头像地址", example = "")
    private String avatar;

    @Schema(description = "用户邮箱", example = "admin@df.com")
    private String email;

    @Schema(description = "手机号码", example = "13800000000")
    private String mobile;

    @Schema(description = "账号状态（0 开启 1 停用）", example = "0")
    private Integer status;

    @Schema(description = "备注", example = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "角色编号列表（仅单条查询返回）", example = "[1]")
    private List<Long> roleIds;
}
