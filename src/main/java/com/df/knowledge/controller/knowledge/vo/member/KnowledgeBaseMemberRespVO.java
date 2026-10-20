package com.df.knowledge.controller.knowledge.vo.member;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库成员 Response VO
 */
@Data
@Schema(description = "知识库成员 Response VO")
public class KnowledgeBaseMemberRespVO {

    @Schema(description = "成员编号", example = "1")
    private Long id;

    @Schema(description = "知识库编号", example = "1")
    private Long knowledgeBaseId;

    @Schema(description = "成员用户编号", example = "2")
    private Long userId;

    @Schema(description = "成员用户账号", example = "lisi")
    private String username;

    @Schema(description = "成员用户昵称", example = "李四")
    private String nickname;

    @Schema(description = "成员角色（1 编辑者 2 查看者）", example = "2")
    private Integer memberRole;

    @Schema(description = "加入时间")
    private LocalDateTime createTime;
}
