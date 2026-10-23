import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
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

it('文档显示格式，彻底删除回收站文档需再次确认', async () => {
  let docs = [
    {
      id: 11,
      knowledgeBaseId: 5,
      title: '旧报告',
      format: 1,
      status: 6,
      deleteTime: new Date().toISOString(),
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
    if (url === '/knowledge/doc/purge') {
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
  fireEvent.click(await screen.findByRole('button', { name: '彻底删除' }))
  expect(request).not.toHaveBeenCalledWith('/knowledge/doc/purge', expect.anything())
  fireEvent.click(await screen.findByRole('button', { name: '确认彻底删除' }))
  await waitFor(() => expect(screen.queryByText('旧报告')).not.toBeInTheDocument())
  expect(request).toHaveBeenCalledWith(
    '/knowledge/doc/purge',
    expect.objectContaining({ params: { id: 11, confirmed: true } }),
  )
})
