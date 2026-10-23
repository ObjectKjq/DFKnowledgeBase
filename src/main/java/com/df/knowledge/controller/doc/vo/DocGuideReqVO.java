package com.df.knowledge.controller.doc.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 引导检索请求，同一问题最多提出两个澄清问题。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DocGuideReqVO extends DocSearchReqVO {
    @NotNull @Positive
    @Schema(description="会话编号")
    private Long conversationId;
    @Schema(description="澄清回答，支持推荐选项或自由输入")
    @Size(max=2000)
    private String answer;
    @Schema(description="立即跳过后续澄清并执行混合检索")
    private boolean skip;
    @Schema(description="命名对话模型；省略沿用会话选择")
    private Long modelId;
}
