package com.df.knowledge.service.rag;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * RAG 答案的分块级引用
 */
@Data
@Accessors(chain = true)
public class RagCitation {

    private Integer number;
    private Long knowledgeBaseId;
    private String knowledgeBaseName;
    private Long docId;
    private String docTitle;
    private Long chunkId;
    private Integer chunkIndex;
    private Integer pageNumber;
    private Integer startOffset;
    private Integer endOffset;
}
