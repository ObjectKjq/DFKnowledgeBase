package com.df.knowledge.controller.rag.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * RAG 引用 Response VO
 */
@Data
@Schema(description = "RAG 引用 Response VO")
public class RagCitationRespVO {

    @Schema(description = "答案中的引用编号")
    private Integer number;

    @Schema(description = "知识库编号")
    private Long knowledgeBaseId;

    @Schema(description = "知识库名称")
    private String knowledgeBaseName;

    @Schema(description = "文档编号")
    private Long docId;

    @Schema(description = "文档标题")
    private String docTitle;

    @Schema(description = "分块编号")
    private Long chunkId;

    @Schema(description = "分块序号")
    private Integer chunkIndex;

    @Schema(description = "页码；非 PDF/PPT 文档为空")
    private Integer pageNumber;

    @Schema(description = "原文起始字符偏移")
    private Integer startOffset;

    @Schema(description = "原文结束字符偏移")
    private Integer endOffset;
}
