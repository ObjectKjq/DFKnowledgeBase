package com.df.knowledge.service.permission;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.df.knowledge.common.PageResult;
import com.df.knowledge.common.exception.ServiceException;
import com.df.knowledge.controller.permission.vo.role.RolePageReqVO;
import com.df.knowledge.controller.permission.vo.role.RoleSaveReqVO;
import com.df.knowledge.dal.dataobject.permission.RoleDO;
import com.df.knowledge.dal.dataobject.permission.RoleMenuDO;
import com.df.knowledge.dal.mysql.permission.RoleMapper;
import com.df.knowledge.dal.mysql.permission.RoleMenuMapper;
import com.df.knowledge.dal.mysql.permission.UserRoleMapper;
import com.df.knowledge.enums.ErrorCodeConstants;
import com.df.knowledge.enums.permission.RoleTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.List;

/**
 * 角色 Service 实现类
 */
@Service
@Validated
@Slf4j
public class RoleServiceImpl implements RoleService {

    @Resource
    private RoleMapper roleMapper;

    @Resource
    private RoleMenuMapper roleMenuMapper;

    @Resource
    private UserRoleMapper userRoleMapper;

    @Resource
    private PermissionService permissionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRole(RoleSaveReqVO reqVO) {
        // 1. 校验角色名、标识唯一
        validateRoleNameUnique(null, reqVO.getName());
        validateRoleCodeUnique(null, reqVO.getCode());
        // 2. 插入角色，类型固定为自定义
        RoleDO role = BeanUtil.toBean(reqVO, RoleDO.class);
        role.setType(RoleTypeEnum.CUSTOM.getCode());
        roleMapper.insert(role);
        return role.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(RoleSaveReqVO reqVO) {
        // 1. 校验角色存在
        validateRoleExists(reqVO.getId());
        // 2. 校验角色名、标识唯一（排除自身）
        validateRoleNameUnique(reqVO.getId(), reqVO.getName());
        validateRoleCodeUnique(reqVO.getId(), reqVO.getCode());
        // 3. 更新角色，类型不允许修改
        RoleDO updateObj = BeanUtil.toBean(reqVO, RoleDO.class);
        updateObj.setType(null);
        roleMapper.updateById(updateObj);
        // 4. 失效使用该角色的用户权限缓存（角色状态可能变化）
        permissionService.resetRoleCache(reqVO.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long id) {
        // 1. 校验角色存在
        RoleDO role = validateRoleExists(id);
        // 2. 内置角色不允许删除
        if (RoleTypeEnum.SYSTEM.getCode().equals(role.getType())) {
            throw new ServiceException(ErrorCodeConstants.ROLE_SYSTEM_TYPE_PROHIBITED);
        }
        // 3. 存在用户使用该角色时不允许删除
        if (CollUtil.isNotEmpty(userRoleMapper.selectListByRoleId(id))) {
            throw new ServiceException(ErrorCodeConstants.ROLE_HAS_USER);
        }
        // 4. 删除角色及其菜单关联
        roleMapper.deleteById(id);
        roleMenuMapper.deleteByRoleId(id);
        // 5. 失效该角色关联用户的权限缓存
        permissionService.resetRoleCache(id);
    }

    @Override
    public RoleDO getRole(Long id) {
        return validateRoleExists(id);
    }

    @Override
    public PageResult<RoleDO> getRolePage(RolePageReqVO reqVO) {
        Page<RoleDO> page = roleMapper.selectPage(reqVO);
        return PageResult.build(page.getTotal(), page.getRecords());
    }

    @Override
    public List<RoleDO> getSimpleRoleList() {
        return roleMapper.selectEnabledList();
    }

    @Override
    public List<Long> getRoleMenuIdList(Long roleId) {
        // 校验角色存在，再返回其菜单编号列表
        validateRoleExists(roleId);
        return roleMenuMapper.selectListByRoleId(roleId).stream()
                .map(RoleMenuDO::getMenuId).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoleMenus(Long roleId, Collection<Long> menuIds) {
        // 1. 校验角色存在
        validateRoleExists(roleId);
        // 2. 先删除旧关联
        roleMenuMapper.deleteByRoleId(roleId);
        // 3. 批量插入新关联
        if (CollUtil.isNotEmpty(menuIds)) {
            menuIds.forEach(menuId -> roleMenuMapper.insert(
                    new RoleMenuDO().setRoleId(roleId).setMenuId(menuId)));
        }
        // 4. 失效该角色关联用户的权限缓存
        permissionService.resetRoleCache(roleId);
    }

    /**
     * 校验角色存在
     *
     * @param id 角色编号
     * @return 角色
     */
    private RoleDO validateRoleExists(Long id) {
        RoleDO role = roleMapper.selectById(id);
        if (role == null) {
            throw new ServiceException(ErrorCodeConstants.ROLE_NOT_FOUND);
        }
        return role;
    }

    /**
     * 校验角色名称唯一
     *
     * @param id 角色编号（排除自身，创建时传 null）
     * @param name 角色名称
     */
    private void validateRoleNameUnique(Long id, String name) {
        RoleDO role = roleMapper.selectByName(name);
        if (role != null && !role.getId().equals(id)) {
            throw new ServiceException(ErrorCodeConstants.ROLE_NAME_DUPLICATE);
        }
    }

    /**
     * 校验角色标识唯一
     *
     * @param id 角色编号（排除自身，创建时传 null）
     * @param code 角色标识
     */
    private void validateRoleCodeUnique(Long id, String code) {
        RoleDO role = roleMapper.selectByCode(code);
        if (role != null && !role.getId().equals(id)) {
            throw new ServiceException(ErrorCodeConstants.ROLE_CODE_DUPLICATE);
        }
    }
}
