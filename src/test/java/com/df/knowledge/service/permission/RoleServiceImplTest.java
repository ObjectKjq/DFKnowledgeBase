package com.df.knowledge.service.permission;

import com.df.knowledge.common.CommonStatusEnum;
import com.df.knowledge.common.exception.ServiceException;
import com.df.knowledge.controller.permission.vo.role.RoleSaveReqVO;
import com.df.knowledge.dal.dataobject.permission.RoleDO;
import com.df.knowledge.dal.dataobject.permission.RoleMenuDO;
import com.df.knowledge.dal.dataobject.permission.UserRoleDO;
import com.df.knowledge.dal.mysql.permission.RoleMapper;
import com.df.knowledge.dal.mysql.permission.RoleMenuMapper;
import com.df.knowledge.dal.mysql.permission.UserRoleMapper;
import com.df.knowledge.enums.ErrorCodeConstants;
import com.df.knowledge.enums.permission.RoleTypeEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link RoleServiceImpl} 的单元测试
 */
@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock
    private RoleMapper roleMapper;

    @Mock
    private RoleMenuMapper roleMenuMapper;

    @Mock
    private UserRoleMapper userRoleMapper;

    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private RoleServiceImpl roleService;

    @Test
    void testCreateRole_nameDuplicate() {
        // 准备参数：角色名已存在
        RoleSaveReqVO reqVO = buildSaveReqVO();
        when(roleMapper.selectByName("运营")).thenReturn(buildRole(1L, RoleTypeEnum.CUSTOM.getCode()));

        // 调用并断言
        ServiceException exception = assertThrows(ServiceException.class, () -> roleService.createRole(reqVO));
        assertEquals(ErrorCodeConstants.ROLE_NAME_DUPLICATE.getCode(), exception.getErrorCode().getCode());
    }

    @Test
    void testCreateRole_codeDuplicate() {
        // 准备参数：角色标识已存在
        RoleSaveReqVO reqVO = buildSaveReqVO();
        when(roleMapper.selectByCode("operator")).thenReturn(buildRole(1L, RoleTypeEnum.CUSTOM.getCode()));

        // 调用并断言
        ServiceException exception = assertThrows(ServiceException.class, () -> roleService.createRole(reqVO));
        assertEquals(ErrorCodeConstants.ROLE_CODE_DUPLICATE.getCode(), exception.getErrorCode().getCode());
    }

    @Test
    void testDeleteRole_systemTypeProhibited() {
        // 准备参数：内置角色
        when(roleMapper.selectById(1L)).thenReturn(buildRole(1L, RoleTypeEnum.SYSTEM.getCode()));

        // 调用并断言
        ServiceException exception = assertThrows(ServiceException.class, () -> roleService.deleteRole(1L));
        assertEquals(ErrorCodeConstants.ROLE_SYSTEM_TYPE_PROHIBITED.getCode(), exception.getErrorCode().getCode());
        verify(roleMapper, never()).deleteById(any(Long.class));
    }

    @Test
    void testDeleteRole_hasUser() {
        // 准备参数：自定义角色但仍被用户使用
        when(roleMapper.selectById(3L)).thenReturn(buildRole(3L, RoleTypeEnum.CUSTOM.getCode()));
        when(userRoleMapper.selectListByRoleId(3L)).thenReturn(List.of(buildUserRole(10L, 3L)));

        // 调用并断言
        ServiceException exception = assertThrows(ServiceException.class, () -> roleService.deleteRole(3L));
        assertEquals(ErrorCodeConstants.ROLE_HAS_USER.getCode(), exception.getErrorCode().getCode());
        verify(roleMapper, never()).deleteById(any(Long.class));
    }

    @Test
    void testDeleteRole_success_clearMenuAndCache() {
        // 准备参数：自定义角色且无用户使用
        when(roleMapper.selectById(3L)).thenReturn(buildRole(3L, RoleTypeEnum.CUSTOM.getCode()));
        when(userRoleMapper.selectListByRoleId(3L)).thenReturn(List.of());

        // 调用
        roleService.deleteRole(3L);

        // 断言：删除角色与菜单关联，并失效权限缓存
        verify(roleMapper).deleteById(3L);
        verify(roleMenuMapper).deleteByRoleId(3L);
        verify(permissionService).resetRoleCache(3L);
    }

    @Test
    void testAssignRoleMenus_resetCache() {
        // 准备参数
        when(roleMapper.selectById(3L)).thenReturn(buildRole(3L, RoleTypeEnum.CUSTOM.getCode()));

        // 调用
        roleService.assignRoleMenus(3L, List.of(1L, 100L));

        // 断言：先清旧关联再写入新关联，并失效权限缓存
        verify(roleMenuMapper).deleteByRoleId(3L);
        verify(roleMenuMapper, times(2)).insert(any(RoleMenuDO.class));
        verify(permissionService).resetRoleCache(3L);
    }

    /**
     * 构造角色创建/修改请求
     */
    private static RoleSaveReqVO buildSaveReqVO() {
        RoleSaveReqVO reqVO = new RoleSaveReqVO();
        reqVO.setName("运营");
        reqVO.setCode("operator");
        reqVO.setSort(3);
        reqVO.setStatus(CommonStatusEnum.ENABLE.getCode());
        return reqVO;
    }

    /**
     * 构造角色 DO
     */
    private static RoleDO buildRole(Long id, Integer type) {
        RoleDO role = new RoleDO();
        role.setId(id);
        role.setType(type);
        role.setStatus(CommonStatusEnum.ENABLE.getCode());
        return role;
    }

    /**
     * 构造用户角色关联 DO
     */
    private static UserRoleDO buildUserRole(Long userId, Long roleId) {
        return new UserRoleDO().setUserId(userId).setRoleId(roleId);
    }
}
