package com.df.knowledge.service.permission;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
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
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 权限 Service 实现类
 */
@Service
@Validated
@Slf4j
public class PermissionServiceImpl implements PermissionService {

    /**
     * 缓存有效期：1 小时
     */
    private static final Duration CACHE_DURATION = Duration.ofHours(1);

    /**
     * 用户角色标识缓存
     */
    private final Cache<Long, List<String>> roleCodeCache = Caffeine.newBuilder()
            .expireAfterWrite(CACHE_DURATION).build();

    /**
     * 用户权限标识缓存
     */
    private final Cache<Long, List<String>> permissionCache = Caffeine.newBuilder()
            .expireAfterWrite(CACHE_DURATION).build();

    /**
     * 用户菜单缓存
     */
    private final Cache<Long, List<MenuDO>> menuCache = Caffeine.newBuilder()
            .expireAfterWrite(CACHE_DURATION).build();

    @Resource
    private UserRoleMapper userRoleMapper;

    @Resource
    private RoleMapper roleMapper;

    @Resource
    private RoleMenuMapper roleMenuMapper;

    @Resource
    private MenuMapper menuMapper;

    @Override
    public List<String> getRoleCodeList(Long userId) {
        return roleCodeCache.get(userId, this::loadRoleCodeList);
    }

    @Override
    public List<String> getPermissionList(Long userId) {
        return permissionCache.get(userId, this::loadPermissionList);
    }

    @Override
    public List<MenuDO> getMenuList(Long userId) {
        return menuCache.get(userId, this::loadMenuList);
    }

    @Override
    public List<MenuDO> getMenuTree(Long userId) {
        return buildMenuTree(getMenuList(userId));
    }

    @Override
    public void resetUserCache(Long userId) {
        // 失效用户维度的三个缓存
        roleCodeCache.invalidate(userId);
        permissionCache.invalidate(userId);
        menuCache.invalidate(userId);
    }

    @Override
    public void resetRoleCache(Long roleId) {
        // 1. 查询使用该角色的用户编号
        List<Long> userIds = userRoleMapper.selectListByRoleId(roleId).stream()
                .map(UserRoleDO::getUserId).distinct().toList();
        // 2. 逐个失效用户的权限缓存
        userIds.forEach(this::resetUserCache);
    }

    @Override
    public void resetAllCache() {
        // allEntries 清空所有缓存，因为涉及多个缓存且无法精确到用户
        roleCodeCache.invalidateAll();
        permissionCache.invalidateAll();
        menuCache.invalidateAll();
    }

    /**
     * 查询用户拥有的开启状态角色列表
     *
     * @param userId 用户编号
     * @return 角色列表
     */
    private List<RoleDO> getEnabledRoleList(Long userId) {
        List<UserRoleDO> userRoles = userRoleMapper.selectListByUserId(userId);
        List<Long> roleIds = userRoles.stream().map(UserRoleDO::getRoleId).distinct().toList();
        if (CollUtil.isEmpty(roleIds)) {
            return Collections.emptyList();
        }
        // 仅开启状态的角色才生效
        return roleMapper.selectListByIds(roleIds).stream()
                .filter(role -> CommonStatusEnum.ENABLE.getCode().equals(role.getStatus()))
                .toList();
    }

    /**
     * 查询用户拥有的菜单编号列表
     *
     * @param userId 用户编号
     * @return 菜单编号列表
     */
    private List<Long> getMenuIdList(Long userId) {
        List<RoleDO> roles = getEnabledRoleList(userId);
        if (CollUtil.isEmpty(roles)) {
            return Collections.emptyList();
        }
        List<Long> roleIds = roles.stream().map(RoleDO::getId).toList();
        return roleMenuMapper.selectListByRoleIds(roleIds).stream()
                .map(RoleMenuDO::getMenuId).distinct().toList();
    }

    /**
     * 加载用户的角色标识列表
     *
     * @param userId 用户编号
     * @return 角色标识列表
     */
    private List<String> loadRoleCodeList(Long userId) {
        return getEnabledRoleList(userId).stream()
                .map(RoleDO::getCode).filter(StrUtil::isNotBlank).toList();
    }

    /**
     * 加载用户的权限标识列表
     *
     * @param userId 用户编号
     * @return 权限标识列表
     */
    private List<String> loadPermissionList(Long userId) {
        List<Long> menuIds = getMenuIdList(userId);
        if (CollUtil.isEmpty(menuIds)) {
            return Collections.emptyList();
        }
        return menuMapper.selectListByIds(menuIds).stream()
                .map(MenuDO::getPermission).filter(StrUtil::isNotBlank).distinct().toList();
    }

    /**
     * 加载用户可访问的菜单列表
     *
     * @param userId 用户编号
     * @return 菜单列表（平铺，按 sort 升序）
     */
    private List<MenuDO> loadMenuList(Long userId) {
        List<Long> menuIds = getMenuIdList(userId);
        if (CollUtil.isEmpty(menuIds)) {
            return Collections.emptyList();
        }
        return menuMapper.selectListByIds(menuIds).stream()
                .filter(menu -> !MenuTypeEnum.BUTTON.getCode().equals(menu.getType()))
                .filter(menu -> CommonStatusEnum.ENABLE.getCode().equals(menu.getStatus()))
                .filter(menu -> Boolean.TRUE.equals(menu.getVisible()))
                .sorted(Comparator.comparing(MenuDO::getSort))
                .toList();
    }

    /**
     * 将平铺菜单列表组装为树
     *
     * 1. 父菜单不在可访问范围内（未授权 / 不可见 / 已停用）时，节点提升为顶层，避免整棵子树丢失
     * 2. 其余节点挂到父菜单的 children 上
     *
     * @param menus 平铺菜单列表（已按 sort 升序）
     * @return 菜单树
     */
    private List<MenuDO> buildMenuTree(List<MenuDO> menus) {
        if (CollUtil.isEmpty(menus)) {
            return Collections.emptyList();
        }
        // 菜单列表来自身份缓存，复制后再挂 children，避免改写缓存对象（否则缓存有效期内二次调用会重复挂载）
        List<MenuDO> copies = menus.stream()
                .map(menu -> BeanUtil.copyProperties(menu, MenuDO.class))
                .toList();
        Map<Long, MenuDO> menuMap = copies.stream()
                .collect(Collectors.toMap(MenuDO::getId, Function.identity()));
        List<MenuDO> roots = new ArrayList<>();
        for (MenuDO menu : copies) {
            MenuDO parent = menu.getParentId() == null ? null : menuMap.get(menu.getParentId());
            if (parent == null) {
                roots.add(menu);
                continue;
            }
            if (parent.getChildren() == null) {
                parent.setChildren(new ArrayList<>());
            }
            parent.getChildren().add(menu);
        }
        return roots;
    }
}
