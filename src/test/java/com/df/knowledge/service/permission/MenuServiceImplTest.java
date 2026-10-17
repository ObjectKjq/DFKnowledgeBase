package com.df.knowledge.service.permission;

import com.df.knowledge.common.exception.ServiceException;
import com.df.knowledge.controller.permission.vo.menu.MenuSaveReqVO;
import com.df.knowledge.dal.dataobject.permission.MenuDO;
import com.df.knowledge.dal.mysql.permission.MenuMapper;
import com.df.knowledge.dal.mysql.permission.RoleMenuMapper;
import com.df.knowledge.enums.ErrorCodeConstants;
import com.df.knowledge.enums.permission.MenuTypeEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link MenuServiceImpl} 的单元测试
 */
@ExtendWith(MockitoExtension.class)
class MenuServiceImplTest {

    @Mock
    private MenuMapper menuMapper;

    @Mock
    private RoleMenuMapper roleMenuMapper;

    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private MenuServiceImpl menuService;

    @Test
    void testCreateMenu_parentNotFound() {
        // 准备参数：父菜单不存在
        MenuSaveReqVO reqVO = buildSaveReqVO(999L);

        // 调用并断言
        ServiceException exception = assertThrows(ServiceException.class, () -> menuService.createMenu(reqVO));
        assertEquals(ErrorCodeConstants.MENU_PARENT_NOT_FOUND.getCode(), exception.getErrorCode().getCode());
        verify(menuMapper, never()).insert(any());
    }

    @Test
    void testUpdateMenu_parentSelf() {
        // 准备参数：父菜单指向自己
        MenuSaveReqVO reqVO = buildSaveReqVO(100L);
        reqVO.setId(100L);
        reqVO.setParentId(100L);
        when(menuMapper.selectById(100L)).thenReturn(buildMenu(100L, MenuTypeEnum.DIR.getCode(), 0L));

        // 调用并断言
        ServiceException exception = assertThrows(ServiceException.class, () -> menuService.updateMenu(reqVO));
        assertEquals(ErrorCodeConstants.MENU_PARENT_ERROR.getCode(), exception.getErrorCode().getCode());
    }

    @Test
    void testDeleteMenu_existsChild() {
        // 准备参数：菜单存在子菜单
        when(menuMapper.selectById(100L)).thenReturn(buildMenu(100L, MenuTypeEnum.DIR.getCode(), 0L));
        when(menuMapper.selectCountByParentId(100L)).thenReturn(1L);

        // 调用并断言
        ServiceException exception = assertThrows(ServiceException.class, () -> menuService.deleteMenu(100L));
        assertEquals(ErrorCodeConstants.MENU_EXISTS_CHILD.getCode(), exception.getErrorCode().getCode());
        verify(menuMapper, never()).deleteById(any());
    }

    @Test
    void testDeleteMenu_success_clearRoleMenuAndCache() {
        // 准备参数：无子菜单
        when(menuMapper.selectById(101L)).thenReturn(buildMenu(101L, MenuTypeEnum.MENU.getCode(), 100L));
        when(menuMapper.selectCountByParentId(101L)).thenReturn(0L);

        // 调用
        menuService.deleteMenu(101L);

        // 断言：删除菜单与角色关联，并清空权限缓存
        verify(menuMapper).deleteById(101L);
        verify(roleMenuMapper).deleteByMenuId(101L);
        verify(permissionService).resetAllCache();
    }

    @Test
    void testUpdateMenu_success_resetAllCache() {
        // 准备参数：父菜单为根
        MenuSaveReqVO reqVO = buildSaveReqVO(0L);
        reqVO.setId(101L);
        when(menuMapper.selectById(101L)).thenReturn(buildMenu(101L, MenuTypeEnum.MENU.getCode(), 100L));

        // 调用
        menuService.updateMenu(reqVO);

        // 断言：菜单变更影响所有用户，清空权限缓存
        verify(menuMapper).updateById(any(MenuDO.class));
        verify(permissionService).resetAllCache();
    }

    /**
     * 构造菜单创建/修改请求
     */
    private static MenuSaveReqVO buildSaveReqVO(Long parentId) {
        MenuSaveReqVO reqVO = new MenuSaveReqVO();
        reqVO.setName("用户管理");
        reqVO.setType(MenuTypeEnum.MENU.getCode());
        reqVO.setSort(1);
        reqVO.setParentId(parentId);
        return reqVO;
    }

    /**
     * 构造菜单 DO
     */
    private static MenuDO buildMenu(Long id, Integer type, Long parentId) {
        MenuDO menu = new MenuDO();
        menu.setId(id);
        menu.setType(type);
        menu.setParentId(parentId);
        menu.setSort(1);
        return menu;
    }
}
