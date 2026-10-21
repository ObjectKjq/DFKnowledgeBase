// @ts-ignore
/* eslint-disable */
import request from '@/lib/http'

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

/** 混合检索 仅检索当前用户有权范围内的可用文档，返回最多 20 个融合排序分块 POST /knowledge/search/search */
export async function search(body: API.DocSearchReqVO, options?: { [key: string]: any }) {
  return request<API.CommonResultListDocChunkRespVO>('/knowledge/search/search', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
