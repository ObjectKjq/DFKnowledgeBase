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
    /**
     * 过滤后融合及重排，并按当前内容权限返回分页结果。
     * @param userId 用户编号
     * @param request 检索过滤及分页参数
     * @return 分页结果及索引中提示
     */
    DocSearchPage search(Long userId, com.df.knowledge.controller.doc.vo.DocSearchReqVO request);

    List<DocChunkResult> search(Long userId, String query, List<Long> selectedKnowledgeBaseIds);

    /**
     * 检查当前用户所选范围内是否存在可检索文档
     *
     * @param userId 用户编号
     * @param selectedKnowledgeBaseIds 选择的知识库范围
     * @return 是否存在已完成索引且可用的文档
     */
    boolean hasAvailableDocuments(Long userId, List<Long> selectedKnowledgeBaseIds);

    /**
     * 按分块编号读取原文与定位信息
     *
     * @param userId 用户编号
     * @param chunkId 分块编号
     * @return 原文与定位信息
     */
    DocChunkResult getPreview(Long userId, Long chunkId);
}
