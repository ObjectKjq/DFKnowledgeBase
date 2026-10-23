// @ts-ignore
/* eslint-disable */
import request from '@/lib/http'

/** 预览整份解析文档 重新授权并读取全部解析分块及页码/工作表位置，提供原始文件兜底 GET /knowledge/search/document-preview */
export async function getDocumentPreview(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getDocumentPreviewParams,
  options?: { [key: string]: any },
) {
  return request<API.CommonResultDocDocumentPreviewRespVO>('/knowledge/search/document-preview', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 引导检索 最多两轮澄清；可跳过，模型失败后说明原因并执行混合检索 POST /knowledge/search/guide */
export async function guide(body: API.DocGuideReqVO, options?: { [key: string]: any }) {
  return request<API.CommonResultDocGuideRespVO>('/knowledge/search/guide', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 预览分块原文 重新校验内容权限和文档可用状态，返回原文及来源定位 GET /knowledge/search/preview */
export async function getPreview(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getPreviewParams,
  options?: { [key: string]: any },
) {
  return request<API.CommonResultDocChunkRespVO>('/knowledge/search/preview', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 混合或模糊检索 按当前权限和文档过滤返回分页，默认每页 20 条；达到候选上限时显式提示缩小范围 POST /knowledge/search/search */
export async function search(body: API.DocSearchReqVO, options?: { [key: string]: any }) {
  return request<API.CommonResultDocSearchPageRespVO>('/knowledge/search/search', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
