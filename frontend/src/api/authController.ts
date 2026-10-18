// @ts-ignore
/* eslint-disable */
import request from '@/lib/http'

/** 获取登录用户的权限信息 一次返回用户信息、角色标识、权限标识与菜单树 GET /system/auth/get-permission-info */
export async function getPermissionInfo(options?: { [key: string]: any }) {
  return request<API.CommonResultAuthPermissionInfoRespVO>('/system/auth/get-permission-info', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 登录 使用账号密码登录，返回访问令牌 POST /system/auth/login */
export async function login(body: API.AuthLoginReqVO, options?: { [key: string]: any }) {
  return request<API.CommonResultAuthLoginRespVO>('/system/auth/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 登出 退出当前登录会话 POST /system/auth/logout */
export async function logout(options?: { [key: string]: any }) {
  return request<API.CommonResultBoolean>('/system/auth/logout', {
    method: 'POST',
    ...(options || {}),
  })
}
