package com.df.knowledge.service.knowledge;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link KnowledgeBaseServiceImpl} 的单元测试
 */
@ExtendWith(MockitoExtension.class)
class KnowledgeBaseServiceImplTest {

    @Mock
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Mock
    private KnowledgeBaseMemberMapper knowledgeBaseMemberMapper;

    @Mock
    private KnowledgeBasePermissionService knowledgeBasePermissionService;

    @InjectMocks
    private KnowledgeBaseServiceImpl knowledgeBaseService;

    @Test
    void testCreateKnowledgeBase_shared_success() {
        // 准备参数：创建共享知识库
        KnowledgeBaseSaveReqVO reqVO = new KnowledgeBaseSaveReqVO();
        reqVO.setName("课题组资料");
        reqVO.setType(KnowledgeBaseTypeEnum.SHARED.getCode());
        reqVO.setDescription("论文与实验记录");
        // mock 插入回填自增主键
        doAnswer(invocation -> {
            KnowledgeBaseDO knowledgeBase = invocation.getArgument(0);
            knowledgeBase.setId(5L);
            return 1;
        }).when(knowledgeBaseMapper).insert(any(KnowledgeBaseDO.class));

        // 调用
        Long knowledgeBaseId = knowledgeBaseService.createKnowledgeBase(10L, reqVO);

        // 断言：类型创建时指定，所有者为当前用户，非默认库
        assertEquals(5L, knowledgeBaseId.longValue());
        ArgumentCaptor<KnowledgeBaseDO> captor = ArgumentCaptor.forClass(KnowledgeBaseDO.class);
        verify(knowledgeBaseMapper).insert(captor.capture());
        KnowledgeBaseDO inserted = captor.getValue();
        assertEquals("课题组资料", inserted.getName());
        assertEquals(KnowledgeBaseTypeEnum.SHARED.getCode(), inserted.getType());
        assertEquals(10L, inserted.getOwnerId().longValue());
        assertEquals(Boolean.FALSE, inserted.getDefaultFlag());
    }

    @Test
    void testCreateDefaultKnowledgeBase_success() {
        // 准备参数：用户创建后自动生成默认知识库
        ArgumentCaptor<KnowledgeBaseDO> captor = ArgumentCaptor.forClass(KnowledgeBaseDO.class);

        // 调用
        knowledgeBaseService.createDefaultKnowledgeBase(10L);

        // 断言：固定名称「我的知识库」、个人类型、标记默认库
        verify(knowledgeBaseMapper).insert(captor.capture());
        KnowledgeBaseDO inserted = captor.getValue();
        assertEquals(KnowledgeBaseConstants.DEFAULT_NAME, inserted.getName());
        assertEquals(KnowledgeBaseTypeEnum.PERSONAL.getCode(), inserted.getType());
        assertEquals(10L, inserted.getOwnerId().longValue());
        assertEquals(Boolean.TRUE, inserted.getDefaultFlag());
    }

    @Test
    void testGetKnowledgeBase_success() {
        // 准备参数
        KnowledgeBaseDO knowledgeBase = buildKnowledgeBase(1L, 10L, KnowledgeBaseTypeEnum.SHARED, false);
        when(knowledgeBaseMapper.selectById(1L)).thenReturn(knowledgeBase);

        // 调用并断言：有权访问时返回知识库
        KnowledgeBaseDO result = knowledgeBaseService.getKnowledgeBase(10L, 1L);
        assertEquals(1L, result.getId().longValue());
    }

    @Test
    void testGetKnowledgeBase_noPermission() {
        // 准备参数：无权访问（非成员）
        doThrow(new ServiceException(ErrorCodeConstants.KB_NOT_FOUND))
                .when(knowledgeBasePermissionService).checkViewPermission(20L, 1L);

        // 调用并断言：拒绝且不泄露知识库存在性
        ServiceException exception = assertThrows(ServiceException.class,
                () -> knowledgeBaseService.getKnowledgeBase(20L, 1L));
        assertEquals(ErrorCodeConstants.KB_NOT_FOUND.getCode(), exception.getErrorCode().getCode());
    }

    @Test
    void testDeleteKnowledgeBase_success() {
        // 准备参数：所有者删除自建知识库

        // 调用
        knowledgeBaseService.deleteKnowledgeBase(10L, 5L);

        // 断言：知识库与其成员关联一并删除
        verify(knowledgeBaseMapper).deleteById(5L);
        verify(knowledgeBaseMemberMapper).deleteByKnowledgeBaseId(5L);
    }

    @Test
    void testDeleteKnowledgeBase_default_notDeletable() {
        // 准备参数：删除默认知识库被权限判定拒绝
        doThrow(new ServiceException(ErrorCodeConstants.KB_DEFAULT_NOT_DELETABLE))
                .when(knowledgeBasePermissionService).checkDeletePermission(10L, 1L);

        // 调用并断言
        ServiceException exception = assertThrows(ServiceException.class,
                () -> knowledgeBaseService.deleteKnowledgeBase(10L, 1L));
        assertEquals(ErrorCodeConstants.KB_DEFAULT_NOT_DELETABLE.getCode(), exception.getErrorCode().getCode());
    }

    @Test
    void testGetKnowledgeBasePage_success() {
        // 准备参数：仅在有权访问的知识库范围内分页
        KnowledgeBasePageReqVO reqVO = new KnowledgeBasePageReqVO();
        when(knowledgeBasePermissionService.getAccessibleKnowledgeBaseIds(10L)).thenReturn(List.of(1L, 2L));
        KnowledgeBaseDO knowledgeBase = buildKnowledgeBase(1L, 10L, KnowledgeBaseTypeEnum.PERSONAL, true);
        Page<KnowledgeBaseDO> page = new Page<>(1L, 10L);
        page.setRecords(List.of(knowledgeBase));
        page.setTotal(1L);
        when(knowledgeBaseMapper.selectPage(reqVO, List.of(1L, 2L))).thenReturn(page);

        // 调用并断言
        PageResult<KnowledgeBaseDO> result = knowledgeBaseService.getKnowledgeBasePage(10L, reqVO);
        assertEquals(1L, result.getTotal().longValue());
        assertEquals(1, result.getList().size());
        assertEquals(1L, result.getList().get(0).getId().longValue());
    }

    @Test
    void testGetKnowledgeBasePage_noAccessible_empty() {
        // 准备参数：无任何有权访问的知识库
        KnowledgeBasePageReqVO reqVO = new KnowledgeBasePageReqVO();
        when(knowledgeBasePermissionService.getAccessibleKnowledgeBaseIds(10L)).thenReturn(List.of());

        // 调用并断言：直接返回空分页，不查询数据库
        PageResult<KnowledgeBaseDO> result = knowledgeBaseService.getKnowledgeBasePage(10L, reqVO);
        assertEquals(0L, result.getTotal().longValue());
        assertTrue(result.getList().isEmpty());
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
}
