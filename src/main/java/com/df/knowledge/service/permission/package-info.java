/**
 * 权限业务：角色、菜单与用户-角色-菜单的权限计算
 *
 * 1. PermissionService 直接依赖 Mapper 做权限查询（避免与 User/Role/Menu Service 循环依赖）
 * 2. 用户权限集合经 Caffeine 缓存（1 小时），由各业务写操作主动失效
 */
package com.df.knowledge.service.permission;
