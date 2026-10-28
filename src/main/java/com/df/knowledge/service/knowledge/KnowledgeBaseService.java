package com.df.knowledge.service.knowledge;

import com.df.knowledge.common.PageResult;
import com.df.knowledge.controller.knowledge.vo.knowledgebase.KnowledgeBasePageReqVO;
import com.df.knowledge.controller.knowledge.vo.knowledgebase.KnowledgeBaseSaveReqVO;
import com.df.knowledge.dal.dataobject.knowledge.KnowledgeBaseDO;

/**
 * 知识库 Service 接口
 */
public interface KnowledgeBaseService {

    /**
     * 创建知识库（创建时指定类型，所有者为当前用户）
     *
     * @param userId 创建者用户编号
     * @param reqVO 创建信息
     * @return 知识库编号
     */
    Long createKnowledgeBase(Long userId, KnowledgeBaseSaveReqVO reqVO);

    /**
     * 创建默认个人知识库「我的知识库」（用户创建时调用，不可删除）
     *
     * @param userId 用户编号
     */
    void createDefaultKnowledgeBase(Long userId);

    /**
     * 删除知识库及其文档和成员关系（默认知识库不可删除）
     *
     * @param userId 操作者用户编号
     * @param id 知识库编号
     */
    void deleteKnowledgeBase(Long userId, Long id);

    /**
     * 获取知识库详情
     *
     * @param userId 操作者用户编号
     * @param id 知识库编号
     * @return 知识库
     */
    KnowledgeBaseDO getKnowledgeBase(Long userId, Long id);

    /**
     * 分页查询当前用户有权访问的知识库
     *
     * @param userId 操作者用户编号
     * @param reqVO 分页查询条件
     * @return 知识库分页
     */
    PageResult<KnowledgeBaseDO> getKnowledgeBasePage(Long userId, KnowledgeBasePageReqVO reqVO);

}
