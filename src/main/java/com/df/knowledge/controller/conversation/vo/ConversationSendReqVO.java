package com.df.knowledge.controller.conversation.vo;

import com.df.knowledge.service.conversation.RetrievalState;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 会话发送请求，包含点击发送时的完整选择快照。 */
@Data
@Schema(description = "会话发送请求")
public class ConversationSendReqVO {

    @Positive
    @Schema(description = "会话编号，首次提问不传，由服务端创建")
    private Long conversationId;

    @NotBlank
    @Size(max = 2000)
    @Schema(description = "本轮用户输入", requiredMode = Schema.RequiredMode.REQUIRED)
    private String question;

    @Valid
    @NotNull
    @Schema(description = "点击发送时的完整选择快照，空数组或空值清除旧选择", requiredMode = Schema.RequiredMode.REQUIRED)
    private RetrievalState state;

    @NotNull
    @Min(0)
    @Schema(description = "客户端已展示的最大消息编号，响应仅返回此编号之后的消息", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long afterMessageId = 0L;

    @Size(max = 2000)
    @Schema(description = "引导检索的原始问题，后续澄清时保持不变")
    private String guideQuestion;

    @Size(max = 2000)
    @Schema(description = "本轮澄清回答")
    private String guideAnswer;

    @Schema(description = "跳过后续澄清，直接混合检索")
    private boolean skipGuide;
}
