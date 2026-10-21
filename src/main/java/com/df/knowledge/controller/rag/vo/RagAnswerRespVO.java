package com.df.knowledge.controller.rag.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * RAG 检索 Response VO
 */
@Data
@Schema(description = "RAG 检索 Response VO")
public class RagAnswerRespVO {

    @Schema(description = "是否已基于充分依据作答")
    private Boolean answered;

    @Schema(description = "答案或拒答说明")
    private String answer;

    @Schema(description = "拒答后的下一步建议")
    private List<String> suggestions;

    @Schema(description = "分块级引用")
    private List<RagCitationRespVO> citations;
}
