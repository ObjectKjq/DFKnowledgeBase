package com.df.knowledge.controller.file.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 文件 Response VO
 */
@Data
@Schema(description = "文件 Response VO")
public class FileRespVO {

    @Schema(description = "文件编号", example = "1")
    private Long id;

    @Schema(description = "文件下载链接（相对路径）", example = "/api/system/file/download?id=1")
    private String url;

    @Schema(description = "上传时的原始文件名", example = "期末复习笔记.pdf")
    private String name;

    @Schema(description = "文件大小（字节）", example = "102400")
    private Long size;

    @Schema(description = "文件 MIME 类型", example = "application/pdf")
    private String type;
}
