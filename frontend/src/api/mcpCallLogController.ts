// @ts-ignore
/* eslint-disable */
import request from '@/lib/http'

/** 分页查看本人 MCP 调用记录 GET /mcp/call-log/page */
export async function page(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.pageParams,
  options?: { [key: string]: any },
) {
  return request<API.CommonResultPageResultMcpCallLogRespVO>('/mcp/call-log/page', {
    method: 'GET',
    params: {
      ...params,
      request: undefined,
      ...params['request'],
    },
    ...(options || {}),
  })
}

/** 管理员查看不含内容的 MCP 统计 GET /mcp/call-log/statistics */
export async function statistics(options?: { [key: string]: any }) {
  return request<API.CommonResultListMapStringObject>('/mcp/call-log/statistics', {
    method: 'GET',
    ...(options || {}),
  })
}
