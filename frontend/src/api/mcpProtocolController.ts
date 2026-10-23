// @ts-ignore
/* eslint-disable */
import request from '@/lib/http'

/** 检查来源并拒绝不支持的 MCP HTTP 方法 GET /mcp */
export async function unsupported(options?: { [key: string]: any }) {
  return request<Record<string, any>>('/mcp', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 检查来源并拒绝不支持的 MCP HTTP 方法 PUT /mcp */
export async function unsupported1(options?: { [key: string]: any }) {
  return request<Record<string, any>>('/mcp', {
    method: 'PUT',
    ...(options || {}),
  })
}

/** MCP 2025-11-25 Streamable HTTP JSON-RPC 接入 POST /mcp */
export async function post(body: string, options?: { [key: string]: any }) {
  return request<Record<string, any>>('/mcp', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 检查来源并拒绝不支持的 MCP HTTP 方法 DELETE /mcp */
export async function unsupported2(options?: { [key: string]: any }) {
  return request<Record<string, any>>('/mcp', {
    method: 'DELETE',
    ...(options || {}),
  })
}

/** 检查来源并拒绝不支持的 MCP HTTP 方法 PATCH /mcp */
export async function unsupported3(options?: { [key: string]: any }) {
  return request<Record<string, any>>('/mcp', {
    method: 'PATCH',
    ...(options || {}),
  })
}
