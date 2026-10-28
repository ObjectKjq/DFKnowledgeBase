package com.df.knowledge.service.knowledge;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.df.knowledge.common.PageResult;
import com.df.knowledge.common.exception.ServiceException;
import com.df.knowledge.controller.knowledge.vo.knowledgebase.KnowledgeBasePageReqVO;
import com.df.knowledge.controller.knowledge.vo.knowledgebase.KnowledgeBaseSaveReqVO;
import com.df.knowledge.dal.dataobject.knowledge.KnowledgeBaseDO;
import com.df.knowledge.dal.mysql.knowledge.KnowledgeBaseMapper;
import com.df.knowledge.dal.mysql.knowledge.KnowledgeBaseMemberMapper;
import com.df.knowledge.enums.ErrorCodeConstants;
import com.df.knowledge.enums.knowledge.KnowledgeBaseConstants;
import com.df.knowledge.enums.knowledge.KnowledgeBaseTypeEnum;
import com.df.knowledge.service.doc.DocService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * 知识库 Service 实现类
 */
@Service
@Validated
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    @Resource
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Resource
    private KnowledgeBaseMemberMapper knowledgeBaseMemberMapper;

    @Resource
    private KnowledgeBasePermissionService knowledgeBasePermissionService;

    @Resource
    private DocService docService;

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
        KnowledgeBaseDO knowledgeBase = knowledgeBaseMapper.selectByIdForUpdate(id);
        if (knowledgeBase == null) {
            throw new ServiceException(ErrorCodeConstants.KB_NOT_FOUND);
        }
        docService.deleteKnowledgeBaseDocs(id);
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
