package com.df.knowledge.enums.doc;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

/**
 * 文档格式枚举（上传格式白名单）
 *
 * 在 code/name 双字段之外携带扩展名集合：格式按家族划分（PRD FR-3：PDF、Word、PPT、Excel、TXT、Markdown），
 * 同一家族的多个扩展名（如 doc/docx）共用一个格式
 */
@Getter
@AllArgsConstructor
public enum DocFormatEnum {

    PDF(1, "PDF", Set.of("pdf")), // PDF 文档
    WORD(2, "Word", Set.of("doc", "docx")), // Word 文档
    PPT(3, "PPT", Set.of("ppt", "pptx")), // PPT 演示文稿
    EXCEL(4, "Excel", Set.of("xls", "xlsx")), // Excel 表格
    TXT(5, "TXT", Set.of("txt")), // 纯文本
    MARKDOWN(6, "Markdown", Set.of("md", "markdown")); // Markdown 文档

    /**
     * 格式
     */
    private final Integer code;

    /**
     * 格式名
     */
    private final String name;

    /**
     * 支持的文件扩展名（小写）
     */
    private final Set<String> extensions;

    /**
     * 按文件扩展名解析文档格式
     *
     * @param extension 文件扩展名（小写）
     * @return 文档格式，白名单外返回 null
     */
    public static DocFormatEnum resolveByExtension(String extension) {
        for (DocFormatEnum format : values()) {
            if (format.getExtensions().contains(extension)) {
                return format;
            }
        }
        return null;
    }
}
