// @ts-ignore
/* eslint-disable */
import request from '@/api/http'

/** 分配角色菜单 整体覆盖该角色的菜单权限，变更立即生效 PUT /system/role/assign-menu */
export async function assignRoleMenus(
  body: API.RoleAssignMenuReqVO,
  options?: { [key: string]: any },
) {
  return request<API.CommonResultBoolean>('/system/role/assign-menu', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 创建角色 POST /system/role/create */
export async function createRole(body: API.RoleSaveReqVO, options?: { [key: string]: any }) {
  return request<API.CommonResultLong>('/system/role/create', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 删除角色 内置角色不允许删除；存在用户使用该角色时不允许删除 DELETE /system/role/delete */
export async function deleteRole(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.deleteRoleParams,
  options?: { [key: string]: any },
) {
  return request<API.CommonResultBoolean>('/system/role/delete', {
    method: 'DELETE',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 获取角色 GET /system/role/get */
export async function getRole(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getRoleParams,
  options?: { [key: string]: any },
) {
  return request<API.CommonResultRoleRespVO>('/system/role/get', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 获取角色拥有的菜单编号列表 供分配菜单时回显已选项 GET /system/role/get-menu-id-list */
export async function getRoleMenuIdList(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getRoleMenuIdListParams,
  options?: { [key: string]: any },
) {
  return request<API.CommonResultListLong>('/system/role/get-menu-id-list', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 获取角色精简列表 仅返回开启状态的角色，供用户分配角色时下拉选择 GET /system/role/list */
export async function getSimpleRoleList(options?: { [key: string]: any }) {
  return request<API.CommonResultListRoleSimpleRespVO>('/system/role/list', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 分页查询角色 支持按名称、标识模糊匹配与状态过滤 GET /system/role/page */
export async function getRolePage(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getRolePageParams,
  options?: { [key: string]: any },
) {
  return request<API.CommonResultPageResultRoleRespVO>('/system/role/page', {
    method: 'GET',
    params: {
      ...params,
      reqVO: undefined,
      ...params['reqVO'],
    },
    ...(options || {}),
  })
}

/** 修改角色 PUT /system/role/update */
export async function updateRole(body: API.RoleSaveReqVO, options?: { [key: string]: any }) {
  return request<API.CommonResultBoolean>('/system/role/update', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
