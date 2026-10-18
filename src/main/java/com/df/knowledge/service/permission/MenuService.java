package com.df.knowledge.service.permission;

import com.df.knowledge.controller.permission.vo.menu.MenuSaveReqVO;
import com.df.knowledge.dal.dataobject.permission.MenuDO;

import java.util.List;

/**
 * 菜单 Service 接口
 */
public interface MenuService {

    /**
     * 创建菜单
     *
     * @param reqVO 创建信息
     * @return 菜单编号
     */
    Long createMenu(MenuSaveReqVO reqVO);

    /**
     * 修改菜单
     *
     * @param reqVO 修改信息（含菜单编号）
     */
    void updateMenu(MenuSaveReqVO reqVO);

    /**
     * 删除菜单
     *
     * 存在子菜单时不允许删除
     *
     * @param id 菜单编号
     */
    void deleteMenu(Long id);

    /**
     * 获取菜单
     *
     * @param id 菜单编号
     * @return 菜单
     */
    MenuDO getMenu(Long id);

    /**
     * 获取全部菜单（平铺，按 sort 升序）
     *
     * @return 菜单列表
     */
    List<MenuDO> getMenuList();
}
