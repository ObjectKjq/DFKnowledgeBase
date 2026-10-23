// @ts-ignore
/* eslint-disable */
import request from '@/lib/http'

/** 获取实例设置 GET /system/settings/get */
export async function get(options?: { [key: string]: any }) {
  return request<API.CommonResultInstanceSettingsRespVO>('/system/settings/get', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 保存实例设置 PUT /system/settings/update */
export async function update(
  body: API.InstanceSettingsSaveReqVO,
  options?: { [key: string]: any },
) {
  return request<API.CommonResultBoolean>('/system/settings/update', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
