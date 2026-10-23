// @ts-ignore
/* eslint-disable */
import request from '@/lib/http'

/** 创建知识库 创建时指定类型（个人/共享），所有者为当前登录用户 POST /knowledge/base/create */
export async function createKnowledgeBase(
  body: API.KnowledgeBaseSaveReqVO,
  options?: { [key: string]: any },
) {
  return request<API.CommonResultLong>('/knowledge/base/create', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 删除知识库 默认知识库不可删除；非成员的请求按知识库不存在拒绝 DELETE /knowledge/base/delete */
export async function deleteKnowledgeBase(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.deleteKnowledgeBaseParams,
  options?: { [key: string]: any },
) {
  return request<API.CommonResultBoolean>('/knowledge/base/delete', {
    method: 'DELETE',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 获取知识库详情 GET /knowledge/base/get */
export async function getKnowledgeBase(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getKnowledgeBaseParams,
  options?: { [key: string]: any },
) {
  return request<API.CommonResultKnowledgeBaseRespVO>('/knowledge/base/get', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 分页查询知识库 仅返回当前用户有权访问的知识库（自己拥有的 + 自己是成员的共享库） GET /knowledge/base/page */
export async function getKnowledgeBasePage(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getKnowledgeBasePageParams,
  options?: { [key: string]: any },
) {
  return request<API.CommonResultPageResultKnowledgeBaseRespVO>('/knowledge/base/page', {
    method: 'GET',
    params: {
      ...params,
      reqVO: undefined,
      ...params['reqVO'],
    },
    ...(options || {}),
  })
}

/** 彻底删除回收站知识库 需要再次确认，不可恢复 DELETE /knowledge/base/purge */
export async function purgeKnowledgeBase(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.purgeKnowledgeBaseParams,
  options?: { [key: string]: any },
) {
  return request<API.CommonResultBoolean>('/knowledge/base/purge', {
    method: 'DELETE',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 查看本人知识库回收站 GET /knowledge/base/recycle-list */
export async function getDeletedKnowledgeBases(options?: { [key: string]: any }) {
  return request<API.CommonResultListKnowledgeBaseRespVO>('/knowledge/base/recycle-list', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 恢复知识库 仅原所有者可在 30 天保留期内恢复，保留原成员关系 POST /knowledge/base/restore */
export async function restoreKnowledgeBase(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.restoreKnowledgeBaseParams,
  options?: { [key: string]: any },
) {
  return request<API.CommonResultBoolean>('/knowledge/base/restore', {
    method: 'POST',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}
