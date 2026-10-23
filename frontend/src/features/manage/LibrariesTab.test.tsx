import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, expect, it, vi } from 'vitest'

import { ModalHost } from '@/components/ModalHost'
import request from '@/lib/http'
import { useAuthStore } from '@/stores/auth'
import { useKb } from '@/stores/knowledge'
import { LibrariesTab } from './LibrariesTab'

vi.mock('@/lib/http', () => ({ default: vi.fn() }))
beforeEach(() => {
  vi.clearAllMocks()
  useAuthStore.setState({ token: 'test', user: { id: 1 } })
  useKb.setState({ modal: null })
})

it('知识库回收站保留成员恢复，并要求再次确认彻底删除', async () => {
  let deleted = [
    { id: 7, name: '旧共享知识库', type: 2, ownerId: 1, deleteTime: new Date().toISOString() },
  ]
  vi.mocked(request).mockImplementation(async (url) => {
    if (url.endsWith('/page')) return { code: 0, data: { list: [], total: 0 } } as never
    if (url.endsWith('/recycle-list')) return { code: 0, data: deleted } as never
    if (url.endsWith('/purge')) {
      deleted = []
      return { code: 0, data: true } as never
    }
    throw new Error(`意外请求 ${url}`)
  })
  render(
    <QueryClientProvider
      client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}
    >
      <LibrariesTab />
      <ModalHost />
    </QueryClientProvider>,
  )
  fireEvent.click(await screen.findByRole('button', { name: '知识库回收站' }))
  expect(await screen.findByText('旧共享知识库')).toBeInTheDocument()
  expect(screen.getByRole('button', { name: '恢复' })).toBeEnabled()
  fireEvent.click(screen.getByRole('button', { name: '彻底删除' }))
  expect(request).not.toHaveBeenCalledWith('/knowledge/base/purge', expect.anything())
  fireEvent.click(await screen.findByRole('button', { name: '确认彻底删除' }))
  await waitFor(() => expect(screen.queryByText('旧共享知识库')).not.toBeInTheDocument())
  expect(request).toHaveBeenCalledWith(
    '/knowledge/base/purge',
    expect.objectContaining({ params: { id: 7, confirmed: true } }),
  )
})
