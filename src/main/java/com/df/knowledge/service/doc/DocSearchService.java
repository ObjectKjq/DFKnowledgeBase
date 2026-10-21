package com.df.knowledge.service.doc;

import java.util.List;

/**
 * 混合检索与原文预览 Service 接口
 */
public interface DocSearchService {

    /**
     * 在当前用户有权范围内检索可用文档
     *
     * @param userId 用户编号
     * @param query 检索文本
     * @param selectedKnowledgeBaseIds 选择的知识库范围，空表示全部有权知识库
     * @return 融合相关性排序的命中分块
     */
    List<DocChunkResult> search(Long userId, String query, List<Long> selectedKnowledgeBaseIds);

    /**
     * 按分块编号读取原文与定位信息
     *
     * @param userId 用户编号
     * @param chunkId 分块编号
     * @return 原文与定位信息
     */
    DocChunkResult getPreview(Long userId, Long chunkId);
}
