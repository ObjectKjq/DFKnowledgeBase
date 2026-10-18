package com.df.knowledge.dal.mysql.permission;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.df.knowledge.dal.dataobject.permission.RoleMenuDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * 角色和菜单关联 Mapper
 */
@Mapper
public interface RoleMenuMapper extends BaseMapper<RoleMenuDO> {

    /**
     * 查询角色拥有的菜单关联列表
     *
     * @param roleId 角色ID
     * @return 菜单关联列表
     */
    default List<RoleMenuDO> selectListByRoleId(Long roleId) {
        return selectList(new LambdaQueryWrapper<RoleMenuDO>().eq(RoleMenuDO::getRoleId, roleId));
    }

    /**
     * 查询多个角色拥有的菜单关联列表
     *
     * @param roleIds 角色ID集合
     * @return 菜单关联列表
     */
    default List<RoleMenuDO> selectListByRoleIds(Collection<Long> roleIds) {
        return selectList(new LambdaQueryWrapper<RoleMenuDO>().in(RoleMenuDO::getRoleId, roleIds));
    }

    /**
     * 删除角色的全部菜单关联
     *
     * @param roleId 角色ID
     */
    default void deleteByRoleId(Long roleId) {
        delete(new LambdaQueryWrapper<RoleMenuDO>().eq(RoleMenuDO::getRoleId, roleId));
    }

    /**
     * 删除菜单的全部角色关联
     *
     * @param menuId 菜单ID
     */
    default void deleteByMenuId(Long menuId) {
        delete(new LambdaQueryWrapper<RoleMenuDO>().eq(RoleMenuDO::getMenuId, menuId));
    }
}
