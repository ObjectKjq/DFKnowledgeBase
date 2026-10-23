package com.df.knowledge.service.doc;

import lombok.Data;

/**
 * 检索命中与原文预览共用的分块定位结果
 */
@Data
public class DocChunkResult {

    private Long knowledgeBaseId;
    private String knowledgeBaseName;
    private Long docId;
    private String docTitle;
    private Long chunkId;
    private Integer chunkIndex;
    private Integer pageNumber;
    private Integer startOffset;
    private Integer endOffset;
    private String text;
    /** Excel 工作表名称，其他格式为空 */
    private String sheetName;

    /** 分块是否来自扫描页 OCR */
    private Boolean ocr;
    /** 实际命中类型：KEYWORD、SEMANTIC 或 HYBRID */
    private String matchType;
    /** 实际索引相关性，混合模式为 RRF 得分 */
    private Double score;
    /** HTML 转义后以 em 标记关键词的原文 */
    private String highlight;
    /** 原始文件编号，下载时仍重新校验授权 */
    private Long fileId;
    /** 当前授权文件下载地址 */
    private String downloadUrl;
    /** PDF 原始页面、TEXT 原文、OFFICE 解析内容 */
    private String previewType;
    /** 文档格式编码 */
    private Integer format;
    /** 文档上传时间 */
    private java.time.LocalDateTime uploadTime;
}
