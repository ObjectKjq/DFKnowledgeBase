package com.df.knowledge.controller.conversation.vo;

import com.df.knowledge.service.conversation.RetrievalState;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/** 修改当前会话选择，不改写既有消息。 */
@Data
public class ConversationStateReqVO {
    @NotNull @Positive
    @Schema(description="会话编号")
    private Long id;
    @NotNull @Valid
    @Schema(description="完整会话当前选择；新选择只影响后续消息")
    private RetrievalState state;
}
