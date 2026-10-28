import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, expect, it, vi } from 'vitest'

import request from '@/lib/http'
import { useAuthStore } from '@/stores/auth'
import { useKb } from '@/stores/knowledge'

import { UploadModal } from './UploadModal'

vi.mock('@/lib/http', () => ({ default: vi.fn() }))

beforeEach(() => {
  vi.clearAllMocks()
  useAuthStore.setState({ token: 'test', user: { id: 1 } })
  useKb.setState({
    modal: { kind: 'upload' },
    pendingUpload: [],
    uploadTarget: '5',
    uploadStarted: false,
  })
})

it('选中文件立即上传，确认创建失败后重试复用文件引用', async () => {
  let attempts = 0
  vi.mocked(request).mockImplementation(async (url) => {
    if (url === '/knowledge/base/page')
      return {
        code: 0,
        data: { list: [{ id: 5, name: '个人知识库', ownerId: 1, type: 1 }], total: 1 },
      } as never
    if (url === '/system/file/upload')
      return { code: 0, data: { id: 9, url: '/system/file/download?id=9' } } as never
    if (url === '/knowledge/doc/create') {
      if (++attempts === 1) throw new Error('创建失败')
      return { code: 0, data: 11 } as never
    }
    throw new Error(`意外请求 ${url}`)
  })
  render(
    <QueryClientProvider
      client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}
    >
      <UploadModal />
    </QueryClientProvider>,
  )
  await screen.findByRole('option', { name: '个人知识库' })
  const file = new File(['example'], 'example.txt', { type: 'text/plain' })
  fireEvent.change(screen.getByLabelText('选择上传文档'), { target: { files: [file] } })
  await screen.findByText('已上传')
  expect(request).toHaveBeenCalledWith(
    '/system/file/upload',
    expect.objectContaining({ data: expect.any(FormData) }),
  )
  expect(
    vi.mocked(request).mock.calls.filter(([url]) => url === '/knowledge/doc/create'),
  ).toHaveLength(0)
  fireEvent.click(screen.getByRole('button', { name: '创建文档 · 1' }))
  await screen.findByRole('alert')
  fireEvent.click(screen.getByRole('button', { name: '创建文档 · 1' }))
  await waitFor(() => expect(useKb.getState().modal).toBeNull())
  expect(request).toHaveBeenCalledWith(
    '/knowledge/doc/create',
    expect.objectContaining({
      data: { knowledgeBaseId: 5, fileId: 9, fileUrl: '/system/file/download?id=9' },
    }),
  )
  expect(
    vi.mocked(request).mock.calls.filter(([url]) => url === '/system/file/upload'),
  ).toHaveLength(1)
})
