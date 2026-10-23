package com.df.knowledge.service.knowledge;

import com.df.knowledge.common.exception.ServiceException;
import com.df.knowledge.dal.dataobject.knowledge.KnowledgeBaseDO;
import com.df.knowledge.dal.dataobject.knowledge.KnowledgeBaseMemberDO;
import com.df.knowledge.dal.mysql.knowledge.KnowledgeBaseMapper;
import com.df.knowledge.dal.mysql.knowledge.KnowledgeBaseMemberMapper;
import com.df.knowledge.enums.ErrorCodeConstants;
import com.df.knowledge.enums.knowledge.KnowledgeBaseMemberRoleEnum;
import com.df.knowledge.enums.knowledge.KnowledgeBaseTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

/**
 * 知识库权限 Service 实现类
 *
 * 权限判定唯一依据 PRD §8.1 权限矩阵：知识库不存在与无权访问统一抛「知识库不存在」，不泄露知识库存在性；
 * 成员权限不足（存在性已知）抛「无权操作该知识库」
 */
@Service
@Validated
@Slf4j
public class KnowledgeBasePermissionServiceImpl implements KnowledgeBasePermissionService {

    @Resource
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Resource
    private KnowledgeBaseMemberMapper knowledgeBaseMemberMapper;

    @Override
    public void checkViewPermission(Long userId, Long knowledgeBaseId) {
        // 查看/检索/问答：所有者与成员均可见
        validateAccessible(userId, knowledgeBaseId);
    }

    @Override
    public void checkContentWritePermission(Long userId, Long knowledgeBaseId) {
        KnowledgeBaseDO knowledgeBase = validateAccessible(userId, knowledgeBaseId);
        // 1. 所有者可写入内容
        if (knowledgeBase.getOwnerId().equals(userId)) {
            return;
        }
        // 2. 仅编辑者可写入内容，查看者只读（未知角色一律按只读拒绝）
        KnowledgeBaseMemberDO member = knowledgeBaseMemberMapper.selectByKnowledgeBaseIdAndUserId(knowledgeBaseId, userId);
        if (!KnowledgeBaseMemberRoleEnum.EDITOR.getCode().equals(member.getMemberRole())) {
            throw new ServiceException(ErrorCodeConstants.KB_NO_PERMISSION);
        }
    }

    @Override
    public void checkMemberManagePermission(Long userId, Long knowledgeBaseId) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            knowledgeBaseMapper.selectByIdForUpdate(knowledgeBaseId);
        }
        KnowledgeBaseDO knowledgeBase = validateAccessible(userId, knowledgeBaseId);
        // 1. 个人知识库无共享概念，不支持成员管理
        if (!KnowledgeBaseTypeEnum.SHARED.getCode().equals(knowledgeBase.getType())) {
            throw new ServiceException(ErrorCodeConstants.KB_PERSONAL_NOT_ALLOW_MEMBER);
        }
        // 2. 仅共享知识库所有者可管理成员
        if (!knowledgeBase.getOwnerId().equals(userId)) {
            throw new ServiceException(ErrorCodeConstants.KB_NO_PERMISSION);
        }
    }

    @Override
    public void checkDeletePermission(Long userId, Long knowledgeBaseId) {
        KnowledgeBaseDO knowledgeBase = validateAccessible(userId, knowledgeBaseId);
        // 1. 仅所有者可删除
        if (!knowledgeBase.getOwnerId().equals(userId)) {
            throw new ServiceException(ErrorCodeConstants.KB_NO_PERMISSION);
        }
        // 2. 默认知识库不可删除
        if (Boolean.TRUE.equals(knowledgeBase.getDefaultFlag())) {
            throw new ServiceException(ErrorCodeConstants.KB_DEFAULT_NOT_DELETABLE);
        }
    }

    @Override
    public List<Long> getAccessibleKnowledgeBaseIds(Long userId) {
        // 1. 自己拥有的知识库
        List<Long> knowledgeBaseIds = new ArrayList<>(knowledgeBaseMapper.selectListByOwnerId(userId)
                .stream().map(KnowledgeBaseDO::getId).toList());
        // 2. 自己作为成员的共享知识库
        knowledgeBaseMemberMapper.selectListByUserId(userId)
                .forEach(member -> knowledgeBaseIds.add(member.getKnowledgeBaseId()));
        // 3. 去重后返回
        return knowledgeBaseIds.stream().distinct().filter(id -> {
            KnowledgeBaseDO knowledgeBase = knowledgeBaseMapper.selectById(id);
            return knowledgeBase != null && knowledgeBase.getDeleteTime() == null;
        }).toList();
    }

    /**
     * 校验用户对知识库可见（所有者或成员）
     *
     * 知识库不存在与无权访问统一按「知识库不存在」拒绝，避免向非成员泄露知识库存在性
     *
     * @param userId 用户编号
     * @param knowledgeBaseId 知识库编号
     * @return 知识库
     */
    private KnowledgeBaseDO validateAccessible(Long userId, Long knowledgeBaseId) {
        KnowledgeBaseDO knowledgeBase = knowledgeBaseMapper.selectById(knowledgeBaseId);
        if (knowledgeBase == null || knowledgeBase.getDeleteTime() != null || (!knowledgeBase.getOwnerId().equals(userId)
                && knowledgeBaseMemberMapper.selectByKnowledgeBaseIdAndUserId(knowledgeBaseId, userId) == null)) {
            throw new ServiceException(ErrorCodeConstants.KB_NOT_FOUND);
        }
        return knowledgeBase;
    }
}
