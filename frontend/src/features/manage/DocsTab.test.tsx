import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, expect, it, vi } from 'vitest'

import { ModalHost } from '@/components/ModalHost'
import request from '@/lib/http'
import { useAuthStore } from '@/stores/auth'
import { useKb } from '@/stores/knowledge'
import { DocsTab } from './DocsTab'

vi.mock('@/lib/http', () => ({ default: vi.fn() }))
beforeEach(() => {
  vi.clearAllMocks()
  useAuthStore.setState({ token: 'test', user: { id: 1 } })
  useKb.setState({ modal: null })
})

it('文档与知识库列表均不轮询，点击查询可刷新当前条件', async () => {
  vi.useFakeTimers()
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  vi.mocked(request).mockResolvedValue({ code: 0, data: { list: [], total: 0 } } as never)
  const view = render(
    <QueryClientProvider client={client}>
      <DocsTab />
    </QueryClientProvider>,
  )
  try {
    await act(async () => {
      await vi.advanceTimersByTimeAsync(10_000)
    })
    const documentRequests = () =>
      vi.mocked(request).mock.calls.filter(([url]) => url === '/knowledge/doc/page')
    expect(documentRequests()).toHaveLength(1)
    expect(
      vi.mocked(request).mock.calls.filter(([url]) => url === '/knowledge/base/page'),
    ).toHaveLength(1)
    fireEvent.click(screen.getByRole('button', { name: '查询' }))
    await act(async () => {
      await vi.advanceTimersByTimeAsync(1)
    })
    expect(documentRequests()).toHaveLength(2)
  } finally {
    view.unmount()
    client.clear()
    vi.useRealTimers()
  }
})

it('文档显示格式，确认删除后直接移除文档', async () => {
  let docs = [
    {
      id: 11,
      knowledgeBaseId: 5,
      title: '旧报告',
      format: 1,
      status: 4,
    },
  ]
  vi.mocked(request).mockImplementation(async (url) => {
    if (url === '/knowledge/base/page')
      return {
        code: 0,
        data: { list: [{ id: 5, name: '个人知识库', ownerId: 1, type: 1 }], total: 1 },
      } as never
    if (url === '/knowledge/doc/page')
      return { code: 0, data: { list: docs, total: docs.length } } as never
    if (url === '/knowledge/doc/delete') {
      docs = []
      return { code: 0, data: true } as never
    }
    throw new Error(`意外请求 ${url}`)
  })
  render(
    <QueryClientProvider
      client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}
    >
      <DocsTab />
      <ModalHost />
    </QueryClientProvider>,
  )
  expect(await screen.findByText('旧报告')).toBeInTheDocument()
  expect(screen.getByRole('columnheader', { name: '格式' })).toBeInTheDocument()
  expect(screen.getByText('PDF')).toBeInTheDocument()
  fireEvent.click(await screen.findByRole('button', { name: '删除' }))
  expect(request).not.toHaveBeenCalledWith('/knowledge/doc/delete', expect.anything())
  fireEvent.click(await screen.findByRole('button', { name: '确认删除' }))
  await waitFor(() => expect(screen.queryByText('旧报告')).not.toBeInTheDocument())
  expect(request).toHaveBeenCalledWith(
    '/knowledge/doc/delete',
    expect.objectContaining({ params: { id: 11 } }),
  )
})
