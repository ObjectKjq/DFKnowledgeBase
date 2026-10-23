package com.df.knowledge.controller.doc.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

/** 引导检索响应，故障时也提供直接混合检索结果。 */
@Data
public class DocGuideRespVO {
    @Schema(description="CLARIFY 澄清、RESULTS 检索完成、FALLBACK 模型故障后直接混合")
    private String stage;
    @Schema(description="已提出的澄清问题轮数，上限 2")
    private int round;
    @Schema(description="澄清问题")
    private String question;
    @Schema(description="推荐选项，仍可自由输入")
    private List<String> options;
    @Schema(description="模型未配置或失败后的中文原因")
    private String reason;
    @Schema(description="真实混合检索分页结果")
    private DocSearchPageRespVO search;
}
