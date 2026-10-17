package com.df.knowledge.service.permission;

import cn.hutool.core.bean.BeanUtil;
import com.df.knowledge.common.exception.ServiceException;
import com.df.knowledge.controller.permission.vo.menu.MenuSaveReqVO;
import com.df.knowledge.dal.dataobject.permission.MenuDO;
import com.df.knowledge.dal.mysql.permission.MenuMapper;
import com.df.knowledge.dal.mysql.permission.RoleMenuMapper;
import com.df.knowledge.enums.ErrorCodeConstants;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Objects;

/**
 * 菜单 Service 实现类
 */
@Service
@Validated
@Slf4j
public class MenuServiceImpl implements MenuService {

    @Resource
    private MenuMapper menuMapper;

    @Resource
    private RoleMenuMapper roleMenuMapper;

    @Resource
    private PermissionService permissionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createMenu(MenuSaveReqVO reqVO) {
        // 1. 校验父菜单
        validateParent(null, reqVO.getParentId());
        // 2. 插入菜单
        MenuDO menu = BeanUtil.toBean(reqVO, MenuDO.class);
        menuMapper.insert(menu);
        return menu.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMenu(MenuSaveReqVO reqVO) {
        // 1. 校验菜单存在
        validateMenuExists(reqVO.getId());
        // 2. 校验父菜单
        validateParent(reqVO.getId(), reqVO.getParentId());
        // 3. 更新菜单
        menuMapper.updateById(BeanUtil.toBean(reqVO, MenuDO.class));
        // 4. 菜单变更影响所有用户的权限，直接清空缓存
        permissionService.resetAllCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMenu(Long id) {
        // 1. 校验菜单存在
        validateMenuExists(id);
        // 2. 存在子菜单时不允许删除
        if (menuMapper.selectCountByParentId(id) > 0) {
            throw new ServiceException(ErrorCodeConstants.MENU_EXISTS_CHILD);
        }
        // 3. 删除菜单及其角色关联
        menuMapper.deleteById(id);
        roleMenuMapper.deleteByMenuId(id);
        // 4. 菜单变更影响所有用户的权限，直接清空缓存
        permissionService.resetAllCache();
    }

    @Override
    public MenuDO getMenu(Long id) {
        return validateMenuExists(id);
    }

    @Override
    public List<MenuDO> getMenuList() {
        return menuMapper.selectList();
    }

    /**
     * 校验菜单存在
     *
     * @param id 菜单编号
     * @return 菜单
     */
    private MenuDO validateMenuExists(Long id) {
        MenuDO menu = menuMapper.selectById(id);
        if (menu == null) {
            throw new ServiceException(ErrorCodeConstants.MENU_NOT_FOUND);
        }
        return menu;
    }

    /**
     * 校验父菜单
     *
     * 1. 父菜单为根（0）时直接通过
     * 2. 父菜单不能是自己
     * 3. 父菜单必须存在
     *
     * @param id 菜单编号（创建时传 null）
     * @param parentId 父菜单编号
     */
    private void validateParent(Long id, Long parentId) {
        if (Objects.equals(parentId, 0L)) {
            return;
        }
        if (Objects.equals(parentId, id)) {
            throw new ServiceException(ErrorCodeConstants.MENU_PARENT_ERROR);
        }
        if (menuMapper.selectById(parentId) == null) {
            throw new ServiceException(ErrorCodeConstants.MENU_PARENT_NOT_FOUND);
        }
    }
}
