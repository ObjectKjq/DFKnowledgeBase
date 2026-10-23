import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, expect, it, vi } from 'vitest'
import { ModalHost } from '@/components/ModalHost'
import request from '@/lib/http'
import { useAuthStore } from '@/stores/auth'
import { useKb } from '@/stores/knowledge'
import { McpTab } from './McpTab'

vi.mock('@/lib/http', () => ({ default: vi.fn() }))
beforeEach(() => {
  vi.clearAllMocks()
  useAuthStore.setState({ token: 'test', user: { id: 1 }, roles: [] })
  useKb.setState({ modal: null })
})

it('创建真实凭证支持工具和1至365天期限，令牌仅当次显示', async () => {
  let credentials: API.McpCredentialRespVO[] = []
  vi.mocked(request).mockImplementation(async (url, options) => {
    if (url.endsWith('/credential/list')) return { code: 0, data: credentials } as never
    if (url.endsWith('/call-log/page')) return { code: 0, data: { list: [], total: 0 } } as never
    if (url.endsWith('/credential/create')) {
      const body = options?.data as API.McpCredentialCreateReqVO
      const created = {
        id: 1,
        name: body.name,
        tools: body.tools,
        expiresAt: '2099-10-01T00:00:00',
      }
      credentials = [created]
      return {
        code: 0,
        data: {
          ...created,
          token: 'dfmcp_once_secret',
          instructions: '使用 Bearer 连接本实例 MCP',
        },
      } as never
    }
    throw new Error(`意外请求 ${url}`)
  })
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(
    <QueryClientProvider client={client}>
      <McpTab />
      <ModalHost />
    </QueryClientProvider>,
  )
  fireEvent.click(screen.getByRole('button', { name: '新建凭证' }))
  expect(screen.getByLabelText('有效天数')).toHaveValue(90)
  expect(screen.queryByLabelText('search_images')).not.toBeInTheDocument()
  fireEvent.change(screen.getByLabelText('凭证名称'), { target: { value: '学习智能体' } })
  fireEvent.change(screen.getByLabelText('有效天数'), { target: { value: '120' } })
  fireEvent.click(screen.getByLabelText('ask'))
  fireEvent.click(screen.getByRole('button', { name: '创建凭证' }))
  expect(await screen.findByText('dfmcp_once_secret')).toBeInTheDocument()
  expect(request).toHaveBeenCalledWith(
    '/mcp/credential/create',
    expect.objectContaining({
      data: { name: '学习智能体', tools: ['search', 'list_knowledge_bases'], validDays: 120 },
    }),
  )
  fireEvent.click(screen.getByRole('button', { name: '已妥善保存' }))
  await waitFor(() => expect(screen.queryByText('dfmcp_once_secret')).not.toBeInTheDocument())
  expect(await screen.findByText('学习智能体')).toBeInTheDocument()
  expect(JSON.stringify(localStorage)).not.toContain('dfmcp_once_secret')
})

it('凭证工具即时保存，撤销需确认并展示真实失败调用记录', async () => {
  let credential: API.McpCredentialRespVO = {
    id: 1,
    name: '只读客户端',
    tools: ['search'],
    expiresAt: '2099-10-01T00:00:00',
  }
  vi.mocked(request).mockImplementation(async (url, options) => {
    if (url.endsWith('/credential/list')) return { code: 0, data: [credential] } as never
    if (url.endsWith('/update-tools')) {
      credential = { ...credential, tools: (options?.data as API.McpCredentialToolsReqVO).tools }
      return { code: 0, data: true } as never
    }
    if (url.endsWith('/revoke')) {
      credential = { ...credential, revokedAt: '2026-10-24T01:00:00' }
      return { code: 0, data: true } as never
    }
    if (url.endsWith('/call-log/page'))
      return {
        code: 0,
        data: {
          list: [
            {
              id: 1,
              credentialId: 1,
              toolName: 'ask',
              outcome: 'REJECTED',
              reasonCode: 'TOOL_DISABLED',
              resultCount: 0,
            },
          ],
          total: 1,
        },
      } as never
    throw new Error(`意外请求 ${url}`)
  })
  render(
    <QueryClientProvider
      client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}
    >
      <McpTab />
      <ModalHost />
    </QueryClientProvider>,
  )
  fireEvent.click(await screen.findByLabelText('只读客户端允许ask'))
  await waitFor(() => expect(credential.tools).toEqual(['search', 'ask']))
  expect(await screen.findByText(/TOOL_DISABLED/)).toBeInTheDocument()
  fireEvent.click(screen.getByRole('button', { name: '撤销' }))
  expect(request).not.toHaveBeenCalledWith('/mcp/credential/revoke', expect.anything())
  fireEvent.click(await screen.findByRole('button', { name: '确认撤销' }))
  expect(await screen.findByText('已撤销')).toBeInTheDocument()
  expect(screen.getByLabelText('只读客户端允许ask')).toBeDisabled()
})
