package com.df.knowledge.controller.doc.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建文档 Request VO（两步式上传：先传文件拿引用，再保存文档，ADR-0009）
 */
@Data
@Schema(description = "创建文档 Request VO")
public class DocCreateReqVO {

    @Schema(description = "知识库编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "知识库编号不能为空")
    private Long knowledgeBaseId;

    @Schema(description = "文件编号（上传接口返回）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "文件编号不能为空")
    private Long fileId;

    @Schema(description = "文件下载链接（上传接口返回）", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "/api/system/file/download?id=1")
    @NotBlank(message = "文件下载链接不能为空")
    private String fileUrl;
}
