package com.df.knowledge.service.knowledge;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.df.knowledge.common.PageResult;
import com.df.knowledge.common.exception.ServiceException;
import com.df.knowledge.controller.knowledge.vo.knowledgebase.KnowledgeBasePageReqVO;
import com.df.knowledge.controller.knowledge.vo.knowledgebase.KnowledgeBaseSaveReqVO;
import com.df.knowledge.dal.dataobject.knowledge.KnowledgeBaseDO;
import com.df.knowledge.dal.mysql.doc.DocMapper;
import com.df.knowledge.dal.mysql.knowledge.KnowledgeBaseMapper;
import com.df.knowledge.dal.mysql.knowledge.KnowledgeBaseMemberMapper;
import com.df.knowledge.enums.ErrorCodeConstants;
import com.df.knowledge.enums.doc.DocStatusEnum;
import com.df.knowledge.enums.knowledge.KnowledgeBaseConstants;
import com.df.knowledge.enums.knowledge.KnowledgeBaseTypeEnum;
import com.df.knowledge.service.doc.DocChunkIndexService;
import com.df.knowledge.service.doc.DocIngestService;
import com.df.knowledge.service.doc.DocService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 知识库 Service 实现类
 */
@Service
@Validated
@Slf4j
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    @Resource
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Resource
    private KnowledgeBaseMemberMapper knowledgeBaseMemberMapper;

    @Resource
    private KnowledgeBasePermissionService knowledgeBasePermissionService;

    @Resource
    private DocMapper docMapper;

    @Resource
    private DocService docService;

    @Resource
    private DocChunkIndexService docChunkIndexService;

    @Resource
    private DocIngestService docIngestService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createKnowledgeBase(Long userId, KnowledgeBaseSaveReqVO reqVO) {
        // 1. 插入知识库：类型创建时指定，所有者为当前用户，非默认库
        KnowledgeBaseDO knowledgeBase = BeanUtil.toBean(reqVO, KnowledgeBaseDO.class);
        knowledgeBase.setOwnerId(userId);
        knowledgeBase.setDefaultFlag(false);
        knowledgeBaseMapper.insert(knowledgeBase);
        return knowledgeBase.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createDefaultKnowledgeBase(Long userId) {
        // 默认个人知识库：固定名称「我的知识库」，标记默认库，不可删除
        KnowledgeBaseDO knowledgeBase = new KnowledgeBaseDO();
        knowledgeBase.setName(KnowledgeBaseConstants.DEFAULT_NAME);
        knowledgeBase.setType(KnowledgeBaseTypeEnum.PERSONAL.getCode());
        knowledgeBase.setOwnerId(userId);
        knowledgeBase.setDefaultFlag(true);
        knowledgeBaseMapper.insert(knowledgeBase);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteKnowledgeBase(Long userId, Long id) {
        // 1. 权限判定：仅所有者可删除，默认知识库不可删除（非成员统一按不存在拒绝）
        knowledgeBasePermissionService.checkDeletePermission(userId, id);
        // 同时隐藏知识库及内容，保留成员关系与文档原状态供恢复。
        KnowledgeBaseDO knowledgeBase = knowledgeBaseMapper.selectByIdForUpdate(id);
        if (knowledgeBase == null || knowledgeBase.getDeleteTime() != null) {
            throw new ServiceException(ErrorCodeConstants.KB_NOT_FOUND);
        }
        for (var doc : docMapper.selectListByKnowledgeBaseId(id)) {
            docChunkIndexService.deleteByDocId(doc.getId());
        }
        knowledgeBase.setDeleteTime(LocalDateTime.now());
        knowledgeBaseMapper.updateById(knowledgeBase);
    }

    @Override
    public List<KnowledgeBaseDO> getDeletedKnowledgeBases(Long userId) {
        return knowledgeBaseMapper.selectDeletedByOwnerId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreKnowledgeBase(Long userId, Long id) {
        KnowledgeBaseDO knowledgeBase = validateDeletedOwner(userId, id);
        if (!knowledgeBase.getDeleteTime().plusDays(30).isAfter(LocalDateTime.now())) {
            throw new ServiceException(ErrorCodeConstants.KB_RESTORE_EXPIRED);
        }
        // 显式置空不受 MyBatis-Plus 非空字段更新策略影响。
        knowledgeBaseMapper.update(null, new LambdaUpdateWrapper<KnowledgeBaseDO>()
                .eq(KnowledgeBaseDO::getId, id).set(KnowledgeBaseDO::getDeleteTime, null));
        var pending = new ArrayList<Long>();
        for (var doc : docMapper.selectListByKnowledgeBaseId(id)) {
            var current = docMapper.selectByIdForUpdate(doc.getId());
            if (current == null) continue;
            if (DocStatusEnum.AVAILABLE.getCode().equals(current.getStatus())) {
                current.setStatus(DocStatusEnum.INDEXING.getCode());
            } else if (DocStatusEnum.isInFlight(current.getStatus())) {
                current.setStatus(DocStatusEnum.QUEUED.getCode());
            } else {
                continue;
            }
            current.setIngestSubmitTime(LocalDateTime.now());
            docMapper.updateById(current);
            pending.add(current.getId());
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override public void afterCommit() { pending.forEach(docIngestService::submit); }
                });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void purgeKnowledgeBase(Long userId, Long id, boolean confirmed) {
        validateDeletedOwner(userId, id);
        if (!confirmed) {
            throw new ServiceException(ErrorCodeConstants.KB_PURGE_CONFIRM_REQUIRED);
        }
        purgeContent(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void purgeExpiredKnowledgeBases() {
        var deadline = LocalDateTime.now().minusDays(30);
        for (var knowledgeBase : knowledgeBaseMapper.selectExpired(deadline)) {
            var current = knowledgeBaseMapper.selectByIdForUpdate(knowledgeBase.getId());
            if (current != null && current.getDeleteTime() != null && !current.getDeleteTime().isAfter(deadline)) {
                purgeContent(current.getId());
            }
        }
    }

    /** 生命周期锁内验证原所有者，已删除知识库不向其他成员透露存在性。 */
    private KnowledgeBaseDO validateDeletedOwner(Long userId, Long id) {
        var knowledgeBase = knowledgeBaseMapper.selectByIdForUpdate(id);
        if (knowledgeBase == null || !userId.equals(knowledgeBase.getOwnerId())) {
            throw new ServiceException(ErrorCodeConstants.KB_NOT_FOUND);
        }
        if (knowledgeBase.getDeleteTime() == null) {
            throw new ServiceException(ErrorCodeConstants.KB_RESTORE_NOT_ALLOWED);
        }
        return knowledgeBase;
    }

    /** 仅在确认手动清理或超期复核之后调用。 */
    private void purgeContent(Long id) {
        docService.purgeKnowledgeBaseDocs(id);
        knowledgeBaseMemberMapper.purgeByKnowledgeBaseId(id);
        knowledgeBaseMapper.purgeById(id);
    }

    @Override
    public KnowledgeBaseDO getKnowledgeBase(Long userId, Long id) {
        // 权限判定：所有者与成员可查看
        knowledgeBasePermissionService.checkViewPermission(userId, id);
        return knowledgeBaseMapper.selectById(id);
    }

    @Override
    public PageResult<KnowledgeBaseDO> getKnowledgeBasePage(Long userId, KnowledgeBasePageReqVO reqVO) {
        // 1. 仅查询当前用户有权访问的知识库（自己拥有 + 自己是成员的共享库）
        List<Long> knowledgeBaseIds = knowledgeBasePermissionService.getAccessibleKnowledgeBaseIds(userId);
        if (CollUtil.isEmpty(knowledgeBaseIds)) {
            return PageResult.empty();
        }
        // 2. 分页查询
        Page<KnowledgeBaseDO> page = knowledgeBaseMapper.selectPage(reqVO, knowledgeBaseIds);
        return PageResult.build(page.getTotal(), page.getRecords());
    }
}
