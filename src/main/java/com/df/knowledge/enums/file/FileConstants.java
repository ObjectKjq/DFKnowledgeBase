package com.df.knowledge.enums.file;

import java.util.List;
import java.util.Map;

/**
 * 文件常量
 */
public interface FileConstants {

    // ========== 上传限制 ==========
    long MAX_FILE_SIZE = 50L * 1024 * 1024; // 单文件大小上限（字节，50 MB）

    // ========== 格式白名单：扩展名（小写）→ 允许的 Content-Type 列表（首项为该扩展名的规范类型） ==========
    // 文档格式取自 {@link com.df.knowledge.enums.doc.DocFormatEnum}（PRD FR-3），图片格式为图片资源预留
    Map<String, List<String>> ALLOWED_CONTENT_TYPES = Map.ofEntries(
            Map.entry("pdf", List.of("application/pdf")),
            Map.entry("doc", List.of("application/msword")),
            Map.entry("docx", List.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document")),
            Map.entry("ppt", List.of("application/vnd.ms-powerpoint")),
            Map.entry("pptx", List.of("application/vnd.openxmlformats-officedocument.presentationml.presentation")),
            Map.entry("xls", List.of("application/vnd.ms-excel")),
            Map.entry("xlsx", List.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
            Map.entry("txt", List.of("text/plain")),
            Map.entry("md", List.of("text/markdown", "text/x-markdown", "text/plain")),
            Map.entry("markdown", List.of("text/markdown", "text/x-markdown", "text/plain")),
            Map.entry("png", List.of("image/png")),
            Map.entry("jpg", List.of("image/jpeg")),
            Map.entry("jpeg", List.of("image/jpeg")),
            Map.entry("gif", List.of("image/gif")),
            Map.entry("webp", List.of("image/webp")));

    // ========== 允许上传的扩展名 ==========
    List<String> ALLOWED_EXTENSIONS = List.copyOf(ALLOWED_CONTENT_TYPES.keySet());
}
