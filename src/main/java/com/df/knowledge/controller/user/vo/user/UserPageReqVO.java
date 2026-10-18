package com.df.knowledge.controller.user.vo.user;

import com.df.knowledge.common.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 管理后台 - 用户分页查询 Request VO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "管理后台 - 用户分页查询 Request VO")
public class UserPageReqVO extends PageParam {

    @Schema(description = "用户账号（模糊匹配）", example = "zhang")
    private String username;

    @Schema(description = "用户昵称（模糊匹配）", example = "张")
    private String nickname;

    @Schema(description = "账号状态（0 开启 1 停用）", example = "0")
    private Integer status;
}
