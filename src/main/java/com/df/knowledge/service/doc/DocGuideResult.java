package com.df.knowledge.service.doc;

import com.df.knowledge.service.conversation.RetrievalState;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/** 引导澄清或已完成的混合检索结果。 */
@Data
public class DocGuideResult {
    /** CLARIFY、RESULTS 或 FALLBACK */
    private String stage;
    /** 实际执行的混合查询，供翻页沿用 */
    @Schema(description = "实际执行的混合查询，供翻页沿用")
    private String query;
    /** 原始用户问题 */
    @Schema(description = "原始用户问题")
    private String originalQuestion;
    /** 本轮范围、过滤、模式及所选上下文的独立快照 */
    @Schema(description = "本轮完整选择快照")
    private RetrievalState state;
    /** 已提出的澄清问题轮数 */
    private int round;
    /** 本轮澄清问题 */
    private String question;
    /** 推荐选项，也允许自由输入 */
    private List<String> options = List.of();
    /** 明确说明转为直接混合检索的原因 */
    private String reason;
    /** 完成或故障后的真实混合检索结果 */
    private DocSearchPage search;
}
