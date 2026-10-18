package com.df.knowledge.service.permission;

import com.df.knowledge.common.PageResult;
import com.df.knowledge.controller.permission.vo.role.RolePageReqVO;
import com.df.knowledge.controller.permission.vo.role.RoleSaveReqVO;
import com.df.knowledge.dal.dataobject.permission.RoleDO;

import java.util.Collection;
import java.util.List;

/**
 * 角色 Service 接口
 */
public interface RoleService {

    /**
     * 创建角色
     *
     * @param reqVO 创建信息
     * @return 角色编号
     */
    Long createRole(RoleSaveReqVO reqVO);

    /**
     * 修改角色
     *
     * @param reqVO 修改信息（含角色编号）
     */
    void updateRole(RoleSaveReqVO reqVO);

    /**
     * 删除角色
     *
     * 内置角色不允许删除；存在用户使用该角色时不允许删除
     *
     * @param id 角色编号
     */
    void deleteRole(Long id);

    /**
     * 获取角色
     *
     * @param id 角色编号
     * @return 角色
     */
    RoleDO getRole(Long id);

    /**
     * 分页查询角色
     *
     * @param reqVO 分页查询条件
     * @return 角色分页
     */
    PageResult<RoleDO> getRolePage(RolePageReqVO reqVO);

    /**
     * 获取开启状态的角色精简列表
     *
     * @return 角色列表
     */
    List<RoleDO> getSimpleRoleList();

    /**
     * 获取角色拥有的菜单编号列表
     *
     * @param roleId 角色编号
     * @return 菜单编号列表
     */
    List<Long> getRoleMenuIdList(Long roleId);

    /**
     * 分配角色的菜单
     *
     * @param roleId 角色编号
     * @param menuIds 菜单编号列表
     */
    void assignRoleMenus(Long roleId, Collection<Long> menuIds);
}
