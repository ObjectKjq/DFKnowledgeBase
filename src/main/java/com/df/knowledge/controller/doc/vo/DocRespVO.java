package com.df.knowledge.controller.doc.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档 Response VO
 */
@Data
@Schema(description = "文档 Response VO")
public class DocRespVO {

    @Schema(description = "文档编号", example = "1")
    private Long id;

    @Schema(description = "知识库编号", example = "1")
    private Long knowledgeBaseId;

    @Schema(description = "文档标题", example = "期末复习笔记.pdf")
    private String title;

    @Schema(description = "上传时的原始文件名", example = "期末复习笔记.pdf")
    private String fileName;

    @Schema(description = "文档格式（1 PDF 2 Word 3 PPT 4 Excel 5 TXT 6 Markdown）", example = "1")
    private Integer format;

    @Schema(description = "文件大小（字节）", example = "102400")
    private Long fileSize;

    @Schema(description = "文档状态（1 排队 2 解析中 3 索引中 4 可用 5 失败 6 已删除）", example = "1")
    private Integer status;

    @Schema(description = "失败原因（保留首次失败原因）", example = "文件损坏")
    private String failReason;

    @Schema(description = "删除时间（软删除 30 天保留期起算点）")
    private LocalDateTime deleteTime;

    @Schema(description = "上传时间")
    private LocalDateTime createTime;

    @Schema(description = "最近状态变更时间")
    private LocalDateTime updateTime;
}
