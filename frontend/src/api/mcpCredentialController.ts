// @ts-ignore
/* eslint-disable */
import request from '@/lib/http'

/** 创建本人 MCP 凭证 POST /mcp/credential/create */
export async function create1(
  body: API.McpCredentialCreateReqVO,
  options?: { [key: string]: any },
) {
  return request<API.CommonResultMcpCredentialCreatedRespVO>('/mcp/credential/create', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 列出本人 MCP 凭证摘要 GET /mcp/credential/list */
export async function list1(options?: { [key: string]: any }) {
  return request<API.CommonResultListMcpCredentialRespVO>('/mcp/credential/list', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 即时撤销本人凭证 POST /mcp/credential/revoke */
export async function revoke(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.revokeParams,
  options?: { [key: string]: any },
) {
  return request<API.CommonResultBoolean>('/mcp/credential/revoke', {
    method: 'POST',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 修改本人凭证工具开关 PUT /mcp/credential/update-tools */
export async function updateTools(
  body: API.McpCredentialToolsReqVO,
  options?: { [key: string]: any },
) {
  return request<API.CommonResultBoolean>('/mcp/credential/update-tools', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
