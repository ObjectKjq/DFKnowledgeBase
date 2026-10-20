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
import com.df.knowledge.enums.knowledge.KnowledgeBaseMemberRoleEnum;
import com.df.knowledge.enums.knowledge.KnowledgeBaseTypeEnum;
import com.df.knowledge.service.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link KnowledgeBaseMemberServiceImpl} 的单元测试
 */
@ExtendWith(MockitoExtension.class)
class KnowledgeBaseMemberServiceImplTest {

    @Mock
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Mock
    private KnowledgeBaseMemberMapper knowledgeBaseMemberMapper;

    @Mock
    private KnowledgeBasePermissionService knowledgeBasePermissionService;

    @Mock
    private UserService userService;

    @InjectMocks
    private KnowledgeBaseMemberServiceImpl knowledgeBaseMemberService;

    @Test
    void testInviteMember_success() {
        // 准备参数：按用户账号邀请成员
        KnowledgeBaseMemberInviteReqVO reqVO = buildInviteReqVO(1L, "lisi",
                KnowledgeBaseMemberRoleEnum.VIEWER);
        when(userService.getUserByUsername("lisi")).thenReturn(buildUser(20L, "lisi"));
        when(knowledgeBaseMapper.selectById(1L)).thenReturn(
                buildKnowledgeBase(1L, 10L, KnowledgeBaseTypeEnum.SHARED, false));
        when(knowledgeBaseMemberMapper.selectByKnowledgeBaseIdAndUserId(1L, 20L)).thenReturn(null);

        // 调用
        knowledgeBaseMemberService.inviteMember(10L, reqVO);

        // 断言：成员关联按指定角色写入
        ArgumentCaptor<KnowledgeBaseMemberDO> captor = ArgumentCaptor.forClass(KnowledgeBaseMemberDO.class);
        verify(knowledgeBaseMemberMapper).insert(captor.capture());
        KnowledgeBaseMemberDO inserted = captor.getValue();
        assertEquals(1L, inserted.getKnowledgeBaseId().longValue());
        assertEquals(20L, inserted.getUserId().longValue());
        assertEquals(KnowledgeBaseMemberRoleEnum.VIEWER.getCode(), inserted.getMemberRole());
    }

    @Test
    void testInviteMember_userNotFound() {
        // 准备参数：被邀请的用户账号不存在
        KnowledgeBaseMemberInviteReqVO reqVO = buildInviteReqVO(1L, "unknown",
                KnowledgeBaseMemberRoleEnum.VIEWER);
        when(userService.getUserByUsername("unknown")).thenReturn(null);

        // 调用并断言
        ServiceException exception = assertThrows(ServiceException.class,
                () -> knowledgeBaseMemberService.inviteMember(10L, reqVO));
        assertEquals(ErrorCodeConstants.USER_NOT_FOUND.getCode(), exception.getErrorCode().getCode());
    }

    @Test
    void testInviteMember_alreadyMember() {
        // 准备参数：被邀请用户已是成员
        KnowledgeBaseMemberInviteReqVO reqVO = buildInviteReqVO(1L, "lisi",
                KnowledgeBaseMemberRoleEnum.EDITOR);
        when(userService.getUserByUsername("lisi")).thenReturn(buildUser(20L, "lisi"));
        when(knowledgeBaseMapper.selectById(1L)).thenReturn(
                buildKnowledgeBase(1L, 10L, KnowledgeBaseTypeEnum.SHARED, false));
        when(knowledgeBaseMemberMapper.selectByKnowledgeBaseIdAndUserId(1L, 20L)).thenReturn(
                buildMember(1L, 20L, KnowledgeBaseMemberRoleEnum.VIEWER));

        // 调用并断言：重复邀请有明确错误
        ServiceException exception = assertThrows(ServiceException.class,
                () -> knowledgeBaseMemberService.inviteMember(10L, reqVO));
        assertEquals(ErrorCodeConstants.KB_MEMBER_EXISTS.getCode(), exception.getErrorCode().getCode());
    }

    @Test
    void testInviteMember_ownerInvited() {
        // 准备参数：被邀请用户是知识库所有者本人
        KnowledgeBaseMemberInviteReqVO reqVO = buildInviteReqVO(1L, "zhangsan",
                KnowledgeBaseMemberRoleEnum.EDITOR);
        when(userService.getUserByUsername("zhangsan")).thenReturn(buildUser(10L, "zhangsan"));
        when(knowledgeBaseMapper.selectById(1L)).thenReturn(
                buildKnowledgeBase(1L, 10L, KnowledgeBaseTypeEnum.SHARED, false));

        // 调用并断言：所有者视为已在库内，拒绝重复邀请
        ServiceException exception = assertThrows(ServiceException.class,
                () -> knowledgeBaseMemberService.inviteMember(10L, reqVO));
        assertEquals(ErrorCodeConstants.KB_MEMBER_EXISTS.getCode(), exception.getErrorCode().getCode());
    }

    @Test
    void testInviteMember_personal_notAllowMember() {
        // 准备参数：个人知识库不允许添加成员
        KnowledgeBaseMemberInviteReqVO reqVO = buildInviteReqVO(1L, "lisi",
                KnowledgeBaseMemberRoleEnum.VIEWER);
        doThrow(new ServiceException(ErrorCodeConstants.KB_PERSONAL_NOT_ALLOW_MEMBER))
                .when(knowledgeBasePermissionService).checkMemberManagePermission(10L, 1L);

        // 调用并断言
        ServiceException exception = assertThrows(ServiceException.class,
                () -> knowledgeBaseMemberService.inviteMember(10L, reqVO));
        assertEquals(ErrorCodeConstants.KB_PERSONAL_NOT_ALLOW_MEMBER.getCode(), exception.getErrorCode().getCode());
    }

    @Test
    void testUpdateMemberRole_success() {
        // 准备参数：把查看者改为编辑者
        KnowledgeBaseMemberUpdateReqVO reqVO = new KnowledgeBaseMemberUpdateReqVO();
        reqVO.setKnowledgeBaseId(1L);
        reqVO.setId(5L);
        reqVO.setMemberRole(KnowledgeBaseMemberRoleEnum.EDITOR.getCode());
        when(knowledgeBaseMemberMapper.selectById(5L)).thenReturn(
                buildMember(1L, 20L, KnowledgeBaseMemberRoleEnum.VIEWER));

        // 调用
        knowledgeBaseMemberService.updateMemberRole(10L, reqVO);

        // 断言：成员角色已更新
        ArgumentCaptor<KnowledgeBaseMemberDO> captor = ArgumentCaptor.forClass(KnowledgeBaseMemberDO.class);
        verify(knowledgeBaseMemberMapper).updateById(captor.capture());
        KnowledgeBaseMemberDO updated = captor.getValue();
        assertEquals(5L, updated.getId().longValue());
        assertEquals(KnowledgeBaseMemberRoleEnum.EDITOR.getCode(), updated.getMemberRole());
    }

    @Test
    void testUpdateMemberRole_memberNotFound() {
        // 准备参数：成员不存在
        KnowledgeBaseMemberUpdateReqVO reqVO = new KnowledgeBaseMemberUpdateReqVO();
        reqVO.setKnowledgeBaseId(1L);
        reqVO.setId(5L);
        reqVO.setMemberRole(KnowledgeBaseMemberRoleEnum.EDITOR.getCode());
        when(knowledgeBaseMemberMapper.selectById(5L)).thenReturn(null);

        // 调用并断言
        ServiceException exception = assertThrows(ServiceException.class,
                () -> knowledgeBaseMemberService.updateMemberRole(10L, reqVO));
        assertEquals(ErrorCodeConstants.KB_MEMBER_NOT_FOUND.getCode(), exception.getErrorCode().getCode());
    }

    @Test
    void testRemoveMember_success() {
        // 准备参数：移除成员
        when(knowledgeBaseMemberMapper.selectById(5L)).thenReturn(
                buildMember(1L, 20L, KnowledgeBaseMemberRoleEnum.VIEWER));

        // 调用
        knowledgeBaseMemberService.removeMember(10L, 1L, 5L);

        // 断言：成员关联已删除
        verify(knowledgeBaseMemberMapper).deleteById(5L);
    }

    @Test
    void testRemoveMember_memberNotFound() {
        // 准备参数：成员不存在
        when(knowledgeBaseMemberMapper.selectById(5L)).thenReturn(null);

        // 调用并断言
        ServiceException exception = assertThrows(ServiceException.class,
                () -> knowledgeBaseMemberService.removeMember(10L, 1L, 5L));
        assertEquals(ErrorCodeConstants.KB_MEMBER_NOT_FOUND.getCode(), exception.getErrorCode().getCode());
    }

    @Test
    void testRemoveMember_notMember_notFound() {
        // 准备参数：操作者不是该知识库的成员（成员是否存在均不泄露）
        doThrow(new ServiceException(ErrorCodeConstants.KB_NOT_FOUND))
                .when(knowledgeBasePermissionService).checkMemberManagePermission(20L, 1L);

        // 调用并断言：先鉴权后定位成员，统一按「知识库不存在」拒绝
        ServiceException exception = assertThrows(ServiceException.class,
                () -> knowledgeBaseMemberService.removeMember(20L, 1L, 5L));
        assertEquals(ErrorCodeConstants.KB_NOT_FOUND.getCode(), exception.getErrorCode().getCode());
    }

    @Test
    void testGetMemberList_success() {
        // 准备参数：知识库有两名成员
        when(knowledgeBaseMemberMapper.selectListByKnowledgeBaseId(1L)).thenReturn(List.of(
                buildMember(1L, 20L, KnowledgeBaseMemberRoleEnum.EDITOR),
                buildMember(1L, 30L, KnowledgeBaseMemberRoleEnum.VIEWER)));

        // 调用并断言
        List<KnowledgeBaseMemberDO> members = knowledgeBaseMemberService.getMemberList(10L, 1L);
        assertEquals(2, members.size());
    }

    /**
     * 构造成员邀请 Request VO
     */
    private static KnowledgeBaseMemberInviteReqVO buildInviteReqVO(Long knowledgeBaseId, String username,
                                                                   KnowledgeBaseMemberRoleEnum memberRole) {
        KnowledgeBaseMemberInviteReqVO reqVO = new KnowledgeBaseMemberInviteReqVO();
        reqVO.setKnowledgeBaseId(knowledgeBaseId);
        reqVO.setUsername(username);
        reqVO.setMemberRole(memberRole.getCode());
        return reqVO;
    }

    /**
     * 构造用户 DO
     */
    private static UserDO buildUser(Long id, String username) {
        UserDO user = new UserDO();
        user.setId(id);
        user.setUsername(username);
        return user;
    }

    /**
     * 构造知识库 DO
     */
    private static KnowledgeBaseDO buildKnowledgeBase(Long id, Long ownerId,
                                                      KnowledgeBaseTypeEnum type, Boolean defaultFlag) {
        KnowledgeBaseDO knowledgeBase = new KnowledgeBaseDO();
        knowledgeBase.setId(id);
        knowledgeBase.setName("知识库");
        knowledgeBase.setType(type.getCode());
        knowledgeBase.setOwnerId(ownerId);
        knowledgeBase.setDefaultFlag(defaultFlag);
        return knowledgeBase;
    }

    /**
     * 构造知识库成员 DO
     */
    private static KnowledgeBaseMemberDO buildMember(Long knowledgeBaseId, Long userId,
                                                     KnowledgeBaseMemberRoleEnum memberRole) {
        KnowledgeBaseMemberDO member = new KnowledgeBaseMemberDO();
        member.setId(5L);
        member.setKnowledgeBaseId(knowledgeBaseId);
        member.setUserId(userId);
        member.setMemberRole(memberRole.getCode());
        return member;
    }
}
