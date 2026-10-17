package com.df.knowledge.service.permission;

import cn.dev33.satoken.stp.StpInterface;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * sa-token 权限数据源实现类
 *
 * sa-token 在校验 {@code @SaCheckPermission} / {@code @SaCheckRole} 时回调本类，从 PermissionService 读取当前用户的权限与角色
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    @Resource
    private PermissionService permissionService;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return permissionService.getPermissionList(Long.valueOf(loginId.toString()));
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return permissionService.getRoleCodeList(Long.valueOf(loginId.toString()));
    }
}
