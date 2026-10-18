// @ts-ignore
/* eslint-disable */
import request from '@/lib/http'

/** 创建用户 创建用户并分配角色 POST /system/user/create */
export async function createUser(body: API.UserSaveReqVO, options?: { [key: string]: any }) {
  return request<API.CommonResultLong>('/system/user/create', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 删除用户 DELETE /system/user/delete */
export async function deleteUser(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.deleteUserParams,
  options?: { [key: string]: any },
) {
  return request<API.CommonResultBoolean>('/system/user/delete', {
    method: 'DELETE',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 获取用户 返回用户资料与其角色编号列表 GET /system/user/get */
export async function getUser(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getUserParams,
  options?: { [key: string]: any },
) {
  return request<API.CommonResultUserRespVO>('/system/user/get', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 分页查询用户 支持按账号、昵称模糊匹配与状态过滤 GET /system/user/page */
export async function getUserPage(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getUserPageParams,
  options?: { [key: string]: any },
) {
  return request<API.CommonResultPageResultUserRespVO>('/system/user/page', {
    method: 'GET',
    params: {
      ...params,
      reqVO: undefined,
      ...params['reqVO'],
    },
    ...(options || {}),
  })
}

/** 获取个人资料 GET /system/user/profile */
export async function getProfile(options?: { [key: string]: any }) {
  return request<API.CommonResultUserRespVO>('/system/user/profile', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 修改个人资料 PUT /system/user/profile/update */
export async function updateProfile(
  body: API.UserProfileUpdateReqVO,
  options?: { [key: string]: any },
) {
  return request<API.CommonResultBoolean>('/system/user/profile/update', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 修改个人密码 需校验旧密码，修改成功后强制下线 PUT /system/user/profile/update-password */
export async function updateProfilePassword(
  body: API.UserPasswordUpdateReqVO,
  options?: { [key: string]: any },
) {
  return request<API.CommonResultBoolean>('/system/user/profile/update-password', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 重置用户密码 管理员重置密码后强制该用户下线 PUT /system/user/reset-password */
export async function resetUserPassword(
  body: API.UserResetPasswordReqVO,
  options?: { [key: string]: any },
) {
  return request<API.CommonResultBoolean>('/system/user/reset-password', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 修改用户 修改用户资料、状态与角色分配 PUT /system/user/update */
export async function updateUser(body: API.UserUpdateReqVO, options?: { [key: string]: any }) {
  return request<API.CommonResultBoolean>('/system/user/update', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 修改用户状态 停用即强制该用户下线 PUT /system/user/update-status */
export async function updateUserStatus(
  body: API.UserUpdateStatusReqVO,
  options?: { [key: string]: any },
) {
  return request<API.CommonResultBoolean>('/system/user/update-status', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
