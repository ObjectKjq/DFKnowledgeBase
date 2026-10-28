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

it('成员弹窗支持 Escape 关闭并保留知识库列表', async () => {
  vi.mocked(request).mockImplementation(async (url) => {
    if (url === '/knowledge/base/page')
      return {
        code: 0,
        data: { list: [{ id: 7, name: '课题知识库', type: 2, ownerId: 1 }], total: 1 },
      } as never
    if (url === '/knowledge/base/member/list') return { code: 0, data: [] } as never
    throw new Error(`意外请求 ${url}`)
  })
  render(
    <QueryClientProvider
      client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}
    >
      <LibrariesTab />
    </QueryClientProvider>,
  )
  fireEvent.click(await screen.findByRole('button', { name: '成员' }))
  const dialog = await screen.findByRole('dialog', { name: '课题知识库 · 成员' })
  fireEvent.keyDown(dialog, { key: 'Escape', keyCode: 27 })
  await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
  expect(screen.getByText('课题知识库')).toBeInTheDocument()
})

it('确认删除后直接移除知识库', async () => {
  let libraries = [{ id: 7, name: '课题知识库', type: 2, ownerId: 1 }]
  vi.mocked(request).mockImplementation(async (url) => {
    if (url === '/knowledge/base/page')
      return { code: 0, data: { list: libraries, total: libraries.length } } as never
    if (url === '/knowledge/base/delete') {
      libraries = []
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
  expect(await screen.findByText('课题知识库')).toBeInTheDocument()
  fireEvent.click(screen.getByRole('button', { name: '删除' }))
  expect(request).not.toHaveBeenCalledWith('/knowledge/base/delete', expect.anything())
  fireEvent.click(await screen.findByRole('button', { name: '确认删除' }))
  await waitFor(() => expect(screen.queryByText('课题知识库')).not.toBeInTheDocument())
  expect(request).toHaveBeenCalledWith(
    '/knowledge/base/delete',
    expect.objectContaining({ params: { id: 7 } }),
  )
})
