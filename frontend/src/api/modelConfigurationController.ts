// @ts-ignore
/* eslint-disable */
import request from '@/lib/http'

/** 新增命名模型 POST /system/model/create */
export async function create(
  body: API.ModelConfigurationSaveReqVO,
  options?: { [key: string]: any },
) {
  return request<API.CommonResultLong>('/system/model/create', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 删除模型 DELETE /system/model/delete */
export async function deleteUsingDelete(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.deleteUsingDELETEParams,
  options?: { [key: string]: any },
) {
  return request<API.CommonResultBoolean>('/system/model/delete', {
    method: 'DELETE',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 查询模型，不返回密钥 GET /system/model/get */
export async function get1(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.get1Params,
  options?: { [key: string]: any },
) {
  return request<API.CommonResultModelConfigurationRespVO>('/system/model/get', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 列出模型配置 GET /system/model/list */
export async function list(options?: { [key: string]: any }) {
  return request<API.CommonResultListModelConfigurationRespVO>('/system/model/list', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 用户可选择的命名对话模型 GET /system/model/simple-list */
export async function simpleList(options?: { [key: string]: any }) {
  return request<API.CommonResultListModelConfigurationSimpleRespVO>('/system/model/simple-list', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 真实测试连接并保存状态和测试时间；模拟项返回 SIMULATED POST /system/model/test */
export async function test(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.testParams,
  options?: { [key: string]: any },
) {
  return request<API.CommonResultModelConfigurationRespVO>('/system/model/test', {
    method: 'POST',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 更新命名模型 PUT /system/model/update */
export async function update1(
  body: API.ModelConfigurationSaveReqVO,
  options?: { [key: string]: any },
) {
  return request<API.CommonResultBoolean>('/system/model/update', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
