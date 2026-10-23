import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen } from '@testing-library/react'
import { beforeEach, expect, it, vi } from 'vitest'

import request from '@/lib/http'
import { useAuthStore } from '@/stores/auth'
import { useChat } from '@/stores/chat'
import { PreviewDrawer } from './PreviewDrawer'

vi.mock('@/lib/http', () => ({ default: vi.fn() }))
beforeEach(() => {
  vi.clearAllMocks()
  useAuthStore.setState({ token: 'test' })
  useChat.setState({ previewChunkId: 21 })
  Element.prototype.scrollIntoView = vi.fn()
  URL.createObjectURL = vi.fn(() => 'blob:preview')
  URL.revokeObjectURL = vi.fn()
})
function renderDrawer() {
  return render(
    <QueryClientProvider
      client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}
    >
      <PreviewDrawer />
    </QueryClientProvider>,
  )
}

it('TXT 预览读取原文件全文并定位命中，关闭后再次打开会重查授权', async () => {
  let allowed = true
  vi.mocked(request).mockImplementation(async (url) => {
    if (!allowed) throw new Error('引用不可用')
    if (url.endsWith('/document-preview'))
      return {
        code: 0,
        data: {
          source: {
            chunkId: 21,
            fileId: 22,
            docTitle: '文本笔记',
            previewType: 'TEXT',
            format: 5,
            startOffset: 5,
            endOffset: 9,
          },
          chunks: [{ chunkId: 21, text: '命中内容' }],
        },
      } as never
    if (url.endsWith('/file/download')) {
      const blob = new Blob(['开头内容\n命中内容\n后续原文'])
      Object.defineProperty(blob, 'text', { value: async () => '开头内容\n命中内容\n后续原文' })
      return blob as never
    }
    throw new Error(`意外请求 ${url}`)
  })
  renderDrawer()
  expect(await screen.findByText(/开头内容/)).toBeInTheDocument()
  expect(screen.getByText('命中内容')).toHaveProperty('tagName', 'MARK')
  expect(screen.getByText(/后续原文/)).toBeInTheDocument()
  expect(screen.getByRole('button', { name: '下载原文件' })).toBeInTheDocument()
  fireEvent.click(screen.getByRole('button', { name: '关闭原文预览' }))
  allowed = false
  useChat.getState().showPreview(21)
  expect(await screen.findByText('引用已不可用')).toBeInTheDocument()
})

it('PDF 展示原始页面并定位命中页，关闭抽屉回收预览地址', async () => {
  vi.mocked(request).mockImplementation(async (url) => {
    if (url.endsWith('/document-preview'))
      return {
        code: 0,
        data: {
          source: {
            chunkId: 21,
            fileId: 22,
            docTitle: '扫描报告',
            previewType: 'PDF',
            format: 1,
            pageNumber: 3,
            ocr: true,
          },
          chunks: [],
        },
      } as never
    if (url.endsWith('/file/download'))
      return new Blob(['%PDF'], { type: 'application/pdf' }) as never
    throw new Error(`意外请求 ${url}`)
  })
  renderDrawer()
  expect(await screen.findByTitle('PDF 原始页面 · 第 3 页')).toHaveAttribute(
    'src',
    'blob:preview#page=3',
  )
  expect(screen.getByText(/OCR 文本/)).toBeInTheDocument()
  fireEvent.click(screen.getByRole('button', { name: '关闭原文预览' }))
  expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:preview')
})

it('Office 展示全部解析内容并保留工作表定位', async () => {
  vi.mocked(request).mockResolvedValue({
    code: 0,
    data: {
      source: {
        chunkId: 21,
        docTitle: '年度数据',
        previewType: 'OFFICE',
        format: 4,
        sheetName: '预算表',
      },
      chunks: [
        { chunkId: 20, text: '其他工作表内容', sheetName: '收入表', format: 4 },
        { chunkId: 21, text: '项目 | 预算\n算力 | 500', sheetName: '预算表', format: 4 },
      ],
    },
  } as never)
  renderDrawer()
  expect(await screen.findByText('其他工作表内容')).toBeInTheDocument()
  expect(screen.getByText(/算力 \| 500/)).toBeInTheDocument()
  expect(screen.getByText(/工作表 收入表/)).toBeInTheDocument()
  expect(screen.getAllByText(/工作表 预算表/).length).toBeGreaterThan(0)
})
