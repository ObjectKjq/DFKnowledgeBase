package com.df.knowledge.service.knowledge;

import java.util.List;

/**
 * 知识库权限 Service 接口
 *
 * 权限判定唯一依据 PRD §8.1 权限矩阵；无权访问统一按「知识库不存在」拒绝，不泄露知识库存在性
 */
public interface KnowledgeBasePermissionService {

    /**
     * 校验查看 / 检索 / 问答权限
     *
     * @param userId 用户编号
     * @param knowledgeBaseId 知识库编号
     */
    void checkViewPermission(Long userId, Long knowledgeBaseId);

    /**
     * 校验内容写入权限（上传文档、删除/恢复文档、重试解析）
     *
     * @param userId 用户编号
     * @param knowledgeBaseId 知识库编号
     */
    void checkContentWritePermission(Long userId, Long knowledgeBaseId);

    /**
     * 校验成员管理权限（邀请/移除成员、更改成员角色）
     *
     * 仅共享知识库所有者可管理成员
     *
     * @param userId 用户编号
     * @param knowledgeBaseId 知识库编号
     */
    void checkMemberManagePermission(Long userId, Long knowledgeBaseId);

    /**
     * 校验删除知识库权限
     *
     * 仅所有者可删除，且默认知识库不可删除
     *
     * @param userId 用户编号
     * @param knowledgeBaseId 知识库编号
     */
    void checkDeletePermission(Long userId, Long knowledgeBaseId);

    /**
     * 获取用户有权访问的知识库编号集合
     *
     * @param userId 用户编号
     * @return 知识库编号集合（自己拥有的 + 自己是成员的共享知识库）
     */
    List<Long> getAccessibleKnowledgeBaseIds(Long userId);
}
