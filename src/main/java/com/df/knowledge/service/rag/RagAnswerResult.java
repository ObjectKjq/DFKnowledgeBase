package com.df.knowledge.service.rag;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * RAG 检索结果
 */
@Data
@Accessors(chain = true)
public class RagAnswerResult {

    private Boolean answered;
    private String answer;
    private List<String> suggestions;
    private List<RagCitation> citations;
}
