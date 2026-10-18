// @ts-ignore
/* eslint-disable */
import request from '@/api/http'

/** 创建菜单 POST /system/menu/create */
export async function createMenu(body: API.MenuSaveReqVO, options?: { [key: string]: any }) {
  return request<API.CommonResultLong>('/system/menu/create', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 删除菜单 存在子菜单时不允许删除 DELETE /system/menu/delete */
export async function deleteMenu(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.deleteMenuParams,
  options?: { [key: string]: any },
) {
  return request<API.CommonResultBoolean>('/system/menu/delete', {
    method: 'DELETE',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 获取菜单 GET /system/menu/get */
export async function getMenu(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getMenuParams,
  options?: { [key: string]: any },
) {
  return request<API.CommonResultMenuRespVO>('/system/menu/get', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 获取全部菜单列表 平铺返回全部菜单（含按钮），按 sort 升序，由前端按 parentId 组装为树 GET /system/menu/list */
export async function getMenuList(options?: { [key: string]: any }) {
  return request<API.CommonResultListMenuRespVO>('/system/menu/list', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 修改菜单 PUT /system/menu/update */
export async function updateMenu(body: API.MenuSaveReqVO, options?: { [key: string]: any }) {
  return request<API.CommonResultBoolean>('/system/menu/update', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
