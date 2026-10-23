import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import request from '@/lib/http'
import { useAuthStore } from '@/stores/auth'
import { ModelsTab } from './ModelsTab'

vi.mock('@/lib/http', () => ({ default: vi.fn() }))

function renderTab() {
  return render(
    <QueryClientProvider
      client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}
    >
      <ModelsTab />
    </QueryClientProvider>,
  )
}

describe('实例模型与运行设置', () => {
  it('管理员修改入库并发和 MCP 总开关并保存运行设置', async () => {
    useAuthStore.setState({ token: 'test-token', roles: ['instance_admin'] })
    vi.mocked(request).mockImplementation(async (url) => {
      if (url.endsWith('/list')) return { code: 0, data: [] } as never
      if (url.endsWith('/settings/get'))
        return {
          code: 0,
          data: {
            mcpEnabled: true,
            mcpRateLimitPerMinute: 60,
            ingestGlobalConcurrency: 2,
            ingestKnowledgeBaseConcurrency: 1,
          },
        } as never
      if (url.endsWith('/settings/update')) return { code: 0, data: true } as never
      throw new Error(`意外请求 ${url}`)
    })
    renderTab()
    fireEvent.change(await screen.findByLabelText('全实例入库并发'), { target: { value: '4' } })
    fireEvent.click(screen.getByLabelText('开启 MCP 接入'))
    fireEvent.click(screen.getByRole('button', { name: '保存运行设置' }))
    expect(await screen.findByText('运行设置已保存')).toBeInTheDocument()
    expect(request).toHaveBeenCalledWith(
      '/system/settings/update',
      expect.objectContaining({
        data: {
          mcpEnabled: false,
          mcpRateLimitPerMinute: 60,
          ingestGlobalConcurrency: 4,
          ingestKnowledgeBaseConcurrency: 1,
        },
      }),
    )
  })
  it('管理员保存命名模型并看到真实连接状态和时间', async () => {
    useAuthStore.setState({ token: 'test-token', roles: ['instance_admin'] })
    let models: API.ModelConfigurationRespVO[] = []
    vi.mocked(request).mockImplementation(async (url, options) => {
      if (url.endsWith('/list')) return { code: 0, data: models } as never
      if (url.endsWith('/settings/get'))
        return {
          code: 0,
          data: {
            mcpEnabled: true,
            mcpRateLimitPerMinute: 60,
            ingestGlobalConcurrency: 2,
            ingestKnowledgeBaseConcurrency: 1,
          },
        } as never
      if (url.endsWith('/create')) {
        models = [
          { ...(options?.data as API.ModelConfigurationSaveReqVO), id: 10, hasApiKey: true },
        ]
        return { code: 0, data: 10 } as never
      }
      if (url.endsWith('/test')) {
        models = [{ ...models[0], testStatus: 'OK', lastTestTime: '2026-10-24T10:15:00' }]
        return { code: 0, data: models[0] } as never
      }
      throw new Error(`意外请求 ${url}`)
    })
    renderTab()
    fireEvent.click(await screen.findByRole('button', { name: '新增模型' }))
    fireEvent.change(screen.getByLabelText('配置名称'), { target: { value: '我的模型' } })
    fireEvent.change(screen.getByLabelText('服务地址'), {
      target: { value: 'http://localhost:1234/v1' },
    })
    fireEvent.change(screen.getByLabelText('提供方模型名'), { target: { value: 'qwen' } })
    fireEvent.change(screen.getByLabelText('API 密钥'), { target: { value: 'secret-key' } })
    fireEvent.click(screen.getByRole('button', { name: '保存模型' }))
    expect(await screen.findByText('我的模型')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '测试连接' }))
    expect(await screen.findByText('OK')).toBeInTheDocument()
    expect(screen.getByText('2026-10-24 10:15:00')).toBeInTheDocument()
    await waitFor(() => expect(screen.queryByDisplayValue('secret-key')).not.toBeInTheDocument())
  })
  beforeEach(() => {
    vi.clearAllMocks()
    useAuthStore.setState({ token: 'test-token', roles: [] })
  })

  it('普通用户只能得到联系管理员提示', () => {
    renderTab()
    expect(
      screen.getByText('模型接入与实例运行设置仅限实例管理员维护，请联系管理员。'),
    ).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: '测试连接' })).not.toBeInTheDocument()
  })
})
