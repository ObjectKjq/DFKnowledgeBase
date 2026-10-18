package com.df.knowledge.service.permission;

import com.df.knowledge.common.CommonStatusEnum;
import com.df.knowledge.dal.dataobject.permission.MenuDO;
import com.df.knowledge.dal.dataobject.permission.RoleDO;
import com.df.knowledge.dal.dataobject.permission.RoleMenuDO;
import com.df.knowledge.dal.dataobject.permission.UserRoleDO;
import com.df.knowledge.dal.mysql.permission.MenuMapper;
import com.df.knowledge.dal.mysql.permission.RoleMapper;
import com.df.knowledge.dal.mysql.permission.RoleMenuMapper;
import com.df.knowledge.dal.mysql.permission.UserRoleMapper;
import com.df.knowledge.enums.permission.MenuTypeEnum;
import com.df.knowledge.enums.permission.RoleTypeEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * {@link PermissionServiceImpl} 的单元测试
 */
@ExtendWith(MockitoExtension.class)
class PermissionServiceImplTest {

    @Mock
    private UserRoleMapper userRoleMapper;

    @Mock
    private RoleMapper roleMapper;

    @Mock
    private RoleMenuMapper roleMenuMapper;

    @Mock
    private MenuMapper menuMapper;

    @InjectMocks
    private PermissionServiceImpl permissionService;

    @Test
    void testGetPermissionList_noRole() {
        // 准备参数：用户未分配角色
        when(userRoleMapper.selectListByUserId(10L)).thenReturn(List.of());

        // 调用并断言：无角色即无权限
        assertTrue(permissionService.getPermissionList(10L).isEmpty());
    }

    @Test
    void testGetPermissionList_onlyEnabledRoleMenus() {
        // 准备参数：用户关联角色 1（开启）与角色 2（停用）
        when(userRoleMapper.selectListByUserId(10L))
                .thenReturn(List.of(buildUserRole(10L, 1L), buildUserRole(10L, 2L)));
        when(roleMapper.selectListByIds(List.of(1L, 2L)))
                .thenReturn(List.of(buildRole(1L, CommonStatusEnum.ENABLE.getCode()),
                        buildRole(2L, CommonStatusEnum.DISABLE.getCode())));
        when(roleMenuMapper.selectListByRoleIds(List.of(1L)))
                .thenReturn(List.of(buildRoleMenu(1L, 100L), buildRoleMenu(1L, 101L)));
        when(menuMapper.selectListByIds(List.of(100L, 101L)))
                .thenReturn(List.of(buildMenu(100L, "system:user:list", MenuTypeEnum.BUTTON, 101L, 1, true),
                        buildMenu(101L, "system:user:create", MenuTypeEnum.BUTTON, 101L, 2, true)));

        // 调用
        List<String> permissions = permissionService.getPermissionList(10L);

        // 断言：仅开启角色的菜单权限串被纳入
        assertEquals(List.of("system:user:list", "system:user:create"), permissions);
    }

    @Test
    void testGetMenuTree_buildTreeAndFilterInvisible() {
        // 准备参数：用户拥有目录 100 与菜单 101，其中菜单 102 不可见
        when(userRoleMapper.selectListByUserId(10L)).thenReturn(List.of(buildUserRole(10L, 1L)));
        when(roleMapper.selectListByIds(List.of(1L)))
                .thenReturn(List.of(buildRole(1L, CommonStatusEnum.ENABLE.getCode())));
        when(roleMenuMapper.selectListByRoleIds(List.of(1L)))
                .thenReturn(List.of(buildRoleMenu(1L, 100L), buildRoleMenu(1L, 101L), buildRoleMenu(1L, 102L)));
        when(menuMapper.selectListByIds(List.of(100L, 101L, 102L)))
                .thenReturn(List.of(
                        buildMenu(100L, "", MenuTypeEnum.DIR, 0L, 1, true),
                        buildMenu(101L, "", MenuTypeEnum.MENU, 100L, 2, true),
                        buildMenu(102L, "", MenuTypeEnum.MENU, 100L, 3, false)));

        // 调用
        List<MenuDO> menuTree = permissionService.getMenuTree(10L);

        // 断言：不可见菜单被过滤，顶级仅目录 100，其子节点为菜单 101
        assertEquals(1, menuTree.size());
        assertEquals(100L, menuTree.get(0).getId().longValue());
        assertEquals(1, menuTree.get(0).getChildren().size());
        assertEquals(101L, menuTree.get(0).getChildren().get(0).getId().longValue());
    }

    /**
     * 构造用户角色关联 DO
     */
    private static UserRoleDO buildUserRole(Long userId, Long roleId) {
        return new UserRoleDO().setUserId(userId).setRoleId(roleId);
    }

    /**
     * 构造角色 DO
     */
    private static RoleDO buildRole(Long id, Integer status) {
        RoleDO role = new RoleDO();
        role.setId(id);
        role.setType(RoleTypeEnum.SYSTEM.getCode());
        role.setStatus(status);
        return role;
    }

    /**
     * 构造角色菜单关联 DO
     */
    private static RoleMenuDO buildRoleMenu(Long roleId, Long menuId) {
        return new RoleMenuDO().setRoleId(roleId).setMenuId(menuId);
    }

    /**
     * 构造菜单 DO
     */
    private static MenuDO buildMenu(Long id, String permission, MenuTypeEnum type, Long parentId,
                                    Integer sort, Boolean visible) {
        MenuDO menu = new MenuDO();
        menu.setId(id);
        menu.setPermission(permission);
        menu.setType(type.getCode());
        menu.setParentId(parentId);
        menu.setSort(sort);
        menu.setVisible(visible);
        menu.setStatus(CommonStatusEnum.ENABLE.getCode());
        return menu;
    }
}
