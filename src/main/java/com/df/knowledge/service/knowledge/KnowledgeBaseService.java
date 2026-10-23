package com.df.knowledge.service.knowledge;

import com.df.knowledge.common.PageResult;
import com.df.knowledge.controller.knowledge.vo.knowledgebase.KnowledgeBasePageReqVO;
import com.df.knowledge.controller.knowledge.vo.knowledgebase.KnowledgeBaseSaveReqVO;
import com.df.knowledge.dal.dataobject.knowledge.KnowledgeBaseDO;

import java.util.List;

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
     * 软删除知识库（默认知识库不可删除，保留成员关系与内容供恢复）
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
     * 分页查询知识库（仅返回用户有权访问的知识库）
     *
     * @param userId 操作者用户编号
     * @param reqVO 分页查询条件
     * @return 知识库分页
     */
    PageResult<KnowledgeBaseDO> getKnowledgeBasePage(Long userId, KnowledgeBasePageReqVO reqVO);
    /** 查看本人知识库回收站。
     * @param userId 原所有者
     * @return 已删除知识库
     */
    List<KnowledgeBaseDO> getDeletedKnowledgeBases(Long userId);

    /** 在 30 天内恢复知识库，保留原成员。
     * @param userId 原所有者
     * @param id 知识库编号
     */
    void restoreKnowledgeBase(Long userId, Long id);

    /** 确认后彻底删除回收站知识库。
     * @param userId 原所有者
     * @param id 知识库编号
     * @param confirmed 是否确认
     */
    void purgeKnowledgeBase(Long userId, Long id, boolean confirmed);

    /** 清理达到保留期的回收站知识库。 */
    void purgeExpiredKnowledgeBases();
}
