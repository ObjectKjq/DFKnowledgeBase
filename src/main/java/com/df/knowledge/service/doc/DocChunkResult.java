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
}
