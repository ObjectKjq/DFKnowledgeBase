package com.df.knowledge.service.knowledge;

import com.df.knowledge.common.exception.ServiceException;
import com.df.knowledge.dal.dataobject.knowledge.KnowledgeBaseDO;
import com.df.knowledge.dal.dataobject.knowledge.KnowledgeBaseMemberDO;
import com.df.knowledge.dal.mysql.knowledge.KnowledgeBaseMapper;
import com.df.knowledge.dal.mysql.knowledge.KnowledgeBaseMemberMapper;
import com.df.knowledge.enums.ErrorCodeConstants;
import com.df.knowledge.enums.knowledge.KnowledgeBaseMemberRoleEnum;
import com.df.knowledge.enums.knowledge.KnowledgeBaseTypeEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * {@link KnowledgeBasePermissionServiceImpl} 的单元测试
 *
 * 覆盖 PRD §8.1 权限矩阵的判定边界：查看/检索/问答、内容写入、成员管理、删除知识库
 */
@ExtendWith(MockitoExtension.class)
class KnowledgeBasePermissionServiceImplTest {

    @Mock
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Mock
    private KnowledgeBaseMemberMapper knowledgeBaseMemberMapper;

    @InjectMocks
    private KnowledgeBasePermissionServiceImpl permissionService;

    @Test
    void testCheckViewPermission_owner_success() {
        // 准备参数：个人知识库所有者
        when(knowledgeBaseMapper.selectById(1L)).thenReturn(
                buildKnowledgeBase(1L, 10L, KnowledgeBaseTypeEnum.PERSONAL, false));

        // 调用：所有者可查看、检索、问答
        permissionService.checkViewPermission(10L, 1L);
    }

    @Test
    void testCheckViewPermission_viewer_success() {
        // 准备参数：共享知识库查看者
        when(knowledgeBaseMapper.selectById(1L)).thenReturn(
                buildKnowledgeBase(1L, 10L, KnowledgeBaseTypeEnum.SHARED, false));
        when(knowledgeBaseMemberMapper.selectByKnowledgeBaseIdAndUserId(1L, 20L)).thenReturn(
                buildMember(1L, 20L, KnowledgeBaseMemberRoleEnum.VIEWER));

        // 调用：查看者可查看、检索、问答
        permissionService.checkViewPermission(20L, 1L);
    }

    @Test
    void testCheckViewPermission_notMember_notFound() {
        // 准备参数：知识库存在，但当前用户既非所有者也非成员
        when(knowledgeBaseMapper.selectById(1L)).thenReturn(
                buildKnowledgeBase(1L, 10L, KnowledgeBaseTypeEnum.SHARED, false));
        when(knowledgeBaseMemberMapper.selectByKnowledgeBaseIdAndUserId(1L, 20L)).thenReturn(null);

        // 调用并断言：统一按「知识库不存在」拒绝，不泄露知识库存在性
        ServiceException exception = assertThrows(ServiceException.class,
                () -> permissionService.checkViewPermission(20L, 1L));
        assertEquals(ErrorCodeConstants.KB_NOT_FOUND.getCode(), exception.getErrorCode().getCode());
    }

    @Test
    void testCheckViewPermission_knowledgeBaseMissing_notFound() {
        // 准备参数：知识库不存在
        when(knowledgeBaseMapper.selectById(1L)).thenReturn(null);

        // 调用并断言
        ServiceException exception = assertThrows(ServiceException.class,
                () -> permissionService.checkViewPermission(20L, 1L));
        assertEquals(ErrorCodeConstants.KB_NOT_FOUND.getCode(), exception.getErrorCode().getCode());
    }

    @Test
    void testCheckContentWritePermission_owner_success() {
        // 准备参数：共享知识库所有者
        when(knowledgeBaseMapper.selectById(1L)).thenReturn(
                buildKnowledgeBase(1L, 10L, KnowledgeBaseTypeEnum.SHARED, false));

        // 调用：所有者可上传、删除/恢复、重试
        permissionService.checkContentWritePermission(10L, 1L);
    }

    @Test
    void testCheckContentWritePermission_editor_success() {
        // 准备参数：共享知识库编辑者
        when(knowledgeBaseMapper.selectById(1L)).thenReturn(
                buildKnowledgeBase(1L, 10L, KnowledgeBaseTypeEnum.SHARED, false));
        when(knowledgeBaseMemberMapper.selectByKnowledgeBaseIdAndUserId(1L, 20L)).thenReturn(
                buildMember(1L, 20L, KnowledgeBaseMemberRoleEnum.EDITOR));

        // 调用：编辑者可上传、删除/恢复、重试
        permissionService.checkContentWritePermission(20L, 1L);
    }

    @Test
    void testCheckContentWritePermission_viewer_noPermission() {
        // 准备参数：共享知识库查看者
        when(knowledgeBaseMapper.selectById(1L)).thenReturn(
                buildKnowledgeBase(1L, 10L, KnowledgeBaseTypeEnum.SHARED, false));
        when(knowledgeBaseMemberMapper.selectByKnowledgeBaseIdAndUserId(1L, 20L)).thenReturn(
                buildMember(1L, 20L, KnowledgeBaseMemberRoleEnum.VIEWER));

        // 调用并断言：查看者上传/删除/重试被拒
        ServiceException exception = assertThrows(ServiceException.class,
                () -> permissionService.checkContentWritePermission(20L, 1L));
        assertEquals(ErrorCodeConstants.KB_NO_PERMISSION.getCode(), exception.getErrorCode().getCode());
    }

    @Test
    void testCheckContentWritePermission_notMember_notFound() {
        // 准备参数：知识库存在，但当前用户不是成员
        when(knowledgeBaseMapper.selectById(1L)).thenReturn(
                buildKnowledgeBase(1L, 10L, KnowledgeBaseTypeEnum.SHARED, false));
        when(knowledgeBaseMemberMapper.selectByKnowledgeBaseIdAndUserId(1L, 20L)).thenReturn(null);

        // 调用并断言：非成员的内容操作被拒，且不泄露知识库存在性
        ServiceException exception = assertThrows(ServiceException.class,
                () -> permissionService.checkContentWritePermission(20L, 1L));
        assertEquals(ErrorCodeConstants.KB_NOT_FOUND.getCode(), exception.getErrorCode().getCode());
    }

    @Test
    void testCheckMemberManagePermission_sharedOwner_success() {
        // 准备参数：共享知识库所有者
        when(knowledgeBaseMapper.selectById(1L)).thenReturn(
                buildKnowledgeBase(1L, 10L, KnowledgeBaseTypeEnum.SHARED, false));

        // 调用：所有者可邀请/移除成员、改角色
        permissionService.checkMemberManagePermission(10L, 1L);
    }

    @Test
    void testCheckMemberManagePermission_editor_noPermission() {
        // 准备参数：共享知识库编辑者
        when(knowledgeBaseMapper.selectById(1L)).thenReturn(
                buildKnowledgeBase(1L, 10L, KnowledgeBaseTypeEnum.SHARED, false));
        when(knowledgeBaseMemberMapper.selectByKnowledgeBaseIdAndUserId(1L, 20L)).thenReturn(
                buildMember(1L, 20L, KnowledgeBaseMemberRoleEnum.EDITOR));

        // 调用并断言：编辑者不可管理成员
        ServiceException exception = assertThrows(ServiceException.class,
                () -> permissionService.checkMemberManagePermission(20L, 1L));
        assertEquals(ErrorCodeConstants.KB_NO_PERMISSION.getCode(), exception.getErrorCode().getCode());
    }

    @Test
    void testCheckMemberManagePermission_personalOwner_notAllowMember() {
        // 准备参数：个人知识库所有者
        when(knowledgeBaseMapper.selectById(1L)).thenReturn(
                buildKnowledgeBase(1L, 10L, KnowledgeBaseTypeEnum.PERSONAL, false));

        // 调用并断言：个人知识库不支持添加成员
        ServiceException exception = assertThrows(ServiceException.class,
                () -> permissionService.checkMemberManagePermission(10L, 1L));
        assertEquals(ErrorCodeConstants.KB_PERSONAL_NOT_ALLOW_MEMBER.getCode(), exception.getErrorCode().getCode());
    }

    @Test
    void testCheckMemberManagePermission_notMember_notFound() {
        // 准备参数：知识库存在，但当前用户不是成员
        when(knowledgeBaseMapper.selectById(1L)).thenReturn(
                buildKnowledgeBase(1L, 10L, KnowledgeBaseTypeEnum.SHARED, false));
        when(knowledgeBaseMemberMapper.selectByKnowledgeBaseIdAndUserId(1L, 20L)).thenReturn(null);

        // 调用并断言：非成员的成员管理被拒，且不泄露知识库存在性
        ServiceException exception = assertThrows(ServiceException.class,
                () -> permissionService.checkMemberManagePermission(20L, 1L));
        assertEquals(ErrorCodeConstants.KB_NOT_FOUND.getCode(), exception.getErrorCode().getCode());
    }

    @Test
    void testCheckDeletePermission_sharedOwner_success() {
        // 准备参数：共享知识库所有者
        when(knowledgeBaseMapper.selectById(1L)).thenReturn(
                buildKnowledgeBase(1L, 10L, KnowledgeBaseTypeEnum.SHARED, false));

        // 调用：所有者可删除自建共享知识库
        permissionService.checkDeletePermission(10L, 1L);
    }

    @Test
    void testCheckDeletePermission_personalOwner_success() {
        // 准备参数：自建个人知识库（非默认库）
        when(knowledgeBaseMapper.selectById(1L)).thenReturn(
                buildKnowledgeBase(1L, 10L, KnowledgeBaseTypeEnum.PERSONAL, false));

        // 调用：自建个人知识库可删除
        permissionService.checkDeletePermission(10L, 1L);
    }

    @Test
    void testCheckDeletePermission_default_notDeletable() {
        // 准备参数：默认个人知识库
        when(knowledgeBaseMapper.selectById(1L)).thenReturn(
                buildKnowledgeBase(1L, 10L, KnowledgeBaseTypeEnum.PERSONAL, true));

        // 调用并断言：默认知识库不允许删除
        ServiceException exception = assertThrows(ServiceException.class,
                () -> permissionService.checkDeletePermission(10L, 1L));
        assertEquals(ErrorCodeConstants.KB_DEFAULT_NOT_DELETABLE.getCode(), exception.getErrorCode().getCode());
    }

    @Test
    void testCheckDeletePermission_editor_noPermission() {
        // 准备参数：共享知识库编辑者
        when(knowledgeBaseMapper.selectById(1L)).thenReturn(
                buildKnowledgeBase(1L, 10L, KnowledgeBaseTypeEnum.SHARED, false));
        when(knowledgeBaseMemberMapper.selectByKnowledgeBaseIdAndUserId(1L, 20L)).thenReturn(
                buildMember(1L, 20L, KnowledgeBaseMemberRoleEnum.EDITOR));

        // 调用并断言：成员（非所有者）不可删除知识库
        ServiceException exception = assertThrows(ServiceException.class,
                () -> permissionService.checkDeletePermission(20L, 1L));
        assertEquals(ErrorCodeConstants.KB_NO_PERMISSION.getCode(), exception.getErrorCode().getCode());
    }

    @Test
    void testCheckDeletePermission_notMember_notFound() {
        // 准备参数：知识库存在，但当前用户不是成员
        when(knowledgeBaseMapper.selectById(1L)).thenReturn(
                buildKnowledgeBase(1L, 10L, KnowledgeBaseTypeEnum.SHARED, false));
        when(knowledgeBaseMemberMapper.selectByKnowledgeBaseIdAndUserId(1L, 20L)).thenReturn(null);

        // 调用并断言：非成员的删除被拒，且不泄露知识库存在性
        ServiceException exception = assertThrows(ServiceException.class,
                () -> permissionService.checkDeletePermission(20L, 1L));
        assertEquals(ErrorCodeConstants.KB_NOT_FOUND.getCode(), exception.getErrorCode().getCode());
    }

    @Test
    void testGetAccessibleKnowledgeBaseIds_success() {
        // 准备参数：自己拥有 1、2 号库，是 2、3 号库的成员
        when(knowledgeBaseMapper.selectListByOwnerId(10L)).thenReturn(List.of(
                buildKnowledgeBase(1L, 10L, KnowledgeBaseTypeEnum.PERSONAL, true),
                buildKnowledgeBase(2L, 10L, KnowledgeBaseTypeEnum.SHARED, false)));
        when(knowledgeBaseMemberMapper.selectListByUserId(10L)).thenReturn(List.of(
                buildMember(2L, 10L, KnowledgeBaseMemberRoleEnum.EDITOR),
                buildMember(3L, 10L, KnowledgeBaseMemberRoleEnum.VIEWER)));

        // 调用并断言：有权访问的知识库去重后返回
        List<Long> knowledgeBaseIds = permissionService.getAccessibleKnowledgeBaseIds(10L);
        assertEquals(List.of(1L, 2L, 3L), knowledgeBaseIds);
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
        member.setKnowledgeBaseId(knowledgeBaseId);
        member.setUserId(userId);
        member.setMemberRole(memberRole.getCode());
        return member;
    }
}
