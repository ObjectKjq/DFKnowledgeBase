package com.df.knowledge.controller.doc.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 检索命中与原文预览的分块 Response VO
 */
@Data
@Schema(description = "分块原文与来源定位")
public class DocChunkRespVO {

    @Schema(description = "知识库编号")
    private Long knowledgeBaseId;

    @Schema(description = "知识库名称")
    private String knowledgeBaseName;

    @Schema(description = "文档编号")
    private Long docId;

    @Schema(description = "文档标题")
    private String docTitle;

    @Schema(description = "分块编号，可用于原文预览")
    private Long chunkId;

    @Schema(description = "文档内块序号，从 0 开始")
    private Integer chunkIndex;

    @Schema(description = "原文页码，从 1 开始；无分页结构时为空")
    private Integer pageNumber;

    @Schema(description = "原文字符起点，含")
    private Integer startOffset;

    @Schema(description = "原文字符终点，不含")
    private Integer endOffset;

    @Schema(description = "命中分块原文")
    private String text;
    /** Excel 工作表名称，其他格式为空 */
    @Schema(description = "Excel 工作表名称")
    private String sheetName;

    /** 分块是否来自扫描页 OCR */
    @Schema(description = "分块是否来自扫描页 OCR")
    private Boolean ocr;
}
