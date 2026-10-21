package com.df.knowledge.controller.conversation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话 Response VO
 */
@Data
@Schema(description = "会话 Response VO")
public class ConversationRespVO {

    @Schema(description = "会话编号", example = "1")
    private Long id;

    @Schema(description = "会话标题")
    private String title;

    @Schema(description = "发起会话时的知识库编号范围，空列表表示全部有权知识库")
    private List<Long> knowledgeBaseIds;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
