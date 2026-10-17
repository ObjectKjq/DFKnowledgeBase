package com.df.knowledge.service.permission;

import com.df.knowledge.dal.dataobject.permission.MenuDO;

import java.util.List;

/**
 * 权限 Service 接口
 *
 * 负责按用户计算其角色标识、权限标识与可访问菜单，并按用户维度缓存
 */
public interface PermissionService {

    /**
     * 获取用户的角色标识列表
     *
     * @param userId 用户编号
     * @return 角色标识列表（仅包含开启状态的角色）
     */
    List<String> getRoleCodeList(Long userId);

    /**
     * 获取用户的权限标识列表
     *
     * @param userId 用户编号
     * @return 权限标识列表（来自其角色关联菜单的 permission 字段）
     */
    List<String> getPermissionList(Long userId);

    /**
     * 获取用户可访问的菜单列表（平铺）
     *
     * @param userId 用户编号
     * @return 菜单列表（不含按钮类型、仅开启且可见的菜单）
     */
    List<MenuDO> getMenuList(Long userId);

    /**
     * 获取用户可访问的菜单树（按 parentId 组装的树形结构）
     *
     * @param userId 用户编号
     * @return 菜单树（不含按钮类型、仅开启且可见的菜单）
     */
    List<MenuDO> getMenuTree(Long userId);

    /**
     * 失效指定用户的权限缓存
     *
     * @param userId 用户编号
     */
    void resetUserCache(Long userId);

    /**
     * 失效使用指定角色的所有用户的权限缓存
     *
     * @param roleId 角色编号
     */
    void resetRoleCache(Long roleId);

    /**
     * 失效全部权限缓存（菜单发生变更时使用）
     */
    void resetAllCache();
}
