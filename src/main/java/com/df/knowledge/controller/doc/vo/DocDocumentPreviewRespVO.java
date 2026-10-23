package com.df.knowledge.controller.doc.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/** 整份文档解析预览响应，Office 不承诺原始排版还原。 */
@Data
public class DocDocumentPreviewRespVO {
    @Schema(description = "原命中分块与原始文件下载元数据")
    private DocChunkRespVO source;
    @Schema(description = "文档全部解析分块，按原文顺序并保留页码、工作表、偏移")
    private List<DocChunkRespVO> chunks;
}
