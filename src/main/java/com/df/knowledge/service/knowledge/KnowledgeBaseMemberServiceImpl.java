package com.df.knowledge.service.knowledge;

import com.df.knowledge.common.exception.ServiceException;
import com.df.knowledge.controller.knowledge.vo.member.KnowledgeBaseMemberInviteReqVO;
import com.df.knowledge.controller.knowledge.vo.member.KnowledgeBaseMemberUpdateReqVO;
import com.df.knowledge.dal.dataobject.knowledge.KnowledgeBaseDO;
import com.df.knowledge.dal.dataobject.knowledge.KnowledgeBaseMemberDO;
import com.df.knowledge.dal.dataobject.user.UserDO;
import com.df.knowledge.dal.mysql.knowledge.KnowledgeBaseMapper;
import com.df.knowledge.dal.mysql.knowledge.KnowledgeBaseMemberMapper;
import com.df.knowledge.enums.ErrorCodeConstants;
import com.df.knowledge.service.user.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * 知识库成员 Service 实现类
 */
@Service
@Validated
@Slf4j
public class KnowledgeBaseMemberServiceImpl implements KnowledgeBaseMemberService {

    @Resource
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Resource
    private KnowledgeBaseMemberMapper knowledgeBaseMemberMapper;

    @Resource
    private KnowledgeBasePermissionService knowledgeBasePermissionService;

    @Resource
    private UserService userService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void inviteMember(Long userId, KnowledgeBaseMemberInviteReqVO reqVO) {
        // 1. 权限判定：仅共享知识库所有者可邀请成员（个人知识库不支持添加成员）
        knowledgeBasePermissionService.checkMemberManagePermission(userId, reqVO.getKnowledgeBaseId());
        // 2. 校验被邀请用户存在
        UserDO user = userService.getUserByUsername(reqVO.getUsername());
        if (user == null) {
            throw new ServiceException(ErrorCodeConstants.USER_NOT_FOUND);
        }
        // 3. 校验未重复邀请（所有者视为已在库内）
        KnowledgeBaseDO knowledgeBase = knowledgeBaseMapper.selectById(reqVO.getKnowledgeBaseId());
        if (knowledgeBase.getOwnerId().equals(user.getId())
                || knowledgeBaseMemberMapper.selectByKnowledgeBaseIdAndUserId(reqVO.getKnowledgeBaseId(), user.getId()) != null) {
            throw new ServiceException(ErrorCodeConstants.KB_MEMBER_EXISTS);
        }
        // 4. 写入成员关联
        KnowledgeBaseMemberDO member = new KnowledgeBaseMemberDO();
        member.setKnowledgeBaseId(reqVO.getKnowledgeBaseId());
        member.setUserId(user.getId());
        member.setMemberRole(reqVO.getMemberRole());
        knowledgeBaseMemberMapper.insert(member);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMemberRole(Long userId, KnowledgeBaseMemberUpdateReqVO reqVO) {
        // 1. 权限判定先行：非成员统一按「知识库不存在」拒绝，不泄露知识库存在性
        knowledgeBasePermissionService.checkMemberManagePermission(userId, reqVO.getKnowledgeBaseId());
        // 2. 校验成员存在且属于该知识库
        KnowledgeBaseMemberDO member = validateMemberExists(reqVO.getKnowledgeBaseId(), reqVO.getId());
        // 3. 更新成员角色
        KnowledgeBaseMemberDO updateObj = new KnowledgeBaseMemberDO();
        updateObj.setId(member.getId());
        updateObj.setMemberRole(reqVO.getMemberRole());
        knowledgeBaseMemberMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeMember(Long userId, Long knowledgeBaseId, Long id) {
        // 1. 权限判定先行：非成员统一按「知识库不存在」拒绝，不泄露知识库存在性
        knowledgeBasePermissionService.checkMemberManagePermission(userId, knowledgeBaseId);
        // 2. 校验成员存在且属于该知识库
        KnowledgeBaseMemberDO member = validateMemberExists(knowledgeBaseId, id);
        // 3. 删除成员关联
        knowledgeBaseMemberMapper.deleteById(member.getId());
    }

    @Override
    public List<KnowledgeBaseMemberDO> getMemberList(Long userId, Long knowledgeBaseId) {
        // 1. 权限判定：知识库成员可查看成员列表
        knowledgeBasePermissionService.checkViewPermission(userId, knowledgeBaseId);
        // 2. 返回成员列表
        return knowledgeBaseMemberMapper.selectListByKnowledgeBaseId(knowledgeBaseId);
    }

    /**
     * 校验成员存在且属于指定知识库
     *
     * @param knowledgeBaseId 知识库编号
     * @param id 成员编号
     * @return 成员
     */
    private KnowledgeBaseMemberDO validateMemberExists(Long knowledgeBaseId, Long id) {
        KnowledgeBaseMemberDO member = knowledgeBaseMemberMapper.selectById(id);
        if (member == null || !knowledgeBaseId.equals(member.getKnowledgeBaseId())) {
            throw new ServiceException(ErrorCodeConstants.KB_MEMBER_NOT_FOUND);
        }
        return member;
    }
}
