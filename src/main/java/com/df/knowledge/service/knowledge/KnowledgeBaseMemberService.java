package com.df.knowledge.service.knowledge;

import com.df.knowledge.controller.knowledge.vo.member.KnowledgeBaseMemberInviteReqVO;
import com.df.knowledge.controller.knowledge.vo.member.KnowledgeBaseMemberUpdateReqVO;
import com.df.knowledge.dal.dataobject.knowledge.KnowledgeBaseMemberDO;

import java.util.List;

/**
 * 知识库成员 Service 接口
 */
public interface KnowledgeBaseMemberService {

    /**
     * 邀请成员（按用户账号邀请并分配成员角色）
     *
     * @param userId 操作者用户编号
     * @param reqVO 邀请信息
     */
    void inviteMember(Long userId, KnowledgeBaseMemberInviteReqVO reqVO);

    /**
     * 更改成员角色
     *
     * @param userId 操作者用户编号
     * @param reqVO 成员角色信息（含成员编号）
     */
    void updateMemberRole(Long userId, KnowledgeBaseMemberUpdateReqVO reqVO);

    /**
     * 移除成员
     *
     * @param userId 操作者用户编号
     * @param knowledgeBaseId 知识库编号
     * @param id 成员编号
     */
    void removeMember(Long userId, Long knowledgeBaseId, Long id);

    /**
     * 获取知识库的成员列表
     *
     * @param userId 操作者用户编号
     * @param knowledgeBaseId 知识库编号
     * @return 成员列表
     */
    List<KnowledgeBaseMemberDO> getMemberList(Long userId, Long knowledgeBaseId);
}
