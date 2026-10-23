import { QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, expect, it, vi } from 'vitest'

import { AppSidebar } from '@/components/AppSidebar'
import request from '@/lib/http'
import { useAuthStore } from '@/stores/auth'
import { useChat } from '@/stores/chat'
import { useKb } from '@/stores/knowledge'
import { queryClient } from '@/lib/queryClient'
import { Composer } from './Composer'
import { MessageList } from './MessageList'

vi.mock('@/lib/http', () => ({
  default: vi.fn(),
  assertResultSuccess: (packet: { code: number; msg?: string }) => {
    if (packet.code !== 0) throw new Error(packet.msg ?? '请求失败')
  },
}))
beforeEach(() => {
  vi.clearAllMocks()
  queryClient.clear()
  useAuthStore.setState({ token: 'test', user: { id: 1 } })
  useChat.setState({
    conversationId: null,
    scope: [],
    mode: '混合检索',
    restoreScope: false,
    busy: false,
    pending: null,
    selectedChunkIds: [],
    filters: { formats: [] },
  })
  useKb.setState({ popover: '' })
  vi.mocked(request).mockImplementation(async (url) => {
    if (url.endsWith('/base/page')) return { code: 0, data: { list: [], total: 0 } } as never
    if (url.endsWith('/simple-list'))
      return { code: 0, data: [{ id: 8, name: '我的对话模型', simulated: true }] } as never
    if (url.endsWith('/conversation/list')) return { code: 0, data: [] } as never
    throw new Error(`意外请求 ${url}`)
  })
})
function renderComposer() {
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <AppSidebar open onClose={() => {}} />
        <MessageList />
        <Composer />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

it('新检索重置为默认混合模式和全部范围', () => {
  useChat.setState({ mode: 'RAG 检索', scope: ['7'] })
  renderComposer()
  fireEvent.click(screen.getByRole('button', { name: /新检索/ }))
  expect(screen.getByRole('button', { name: '混合检索' })).toBeInTheDocument()
  expect(screen.getByRole('button', { name: '检索范围：全部知识库' })).toBeInTheDocument()
})

it('输入区仅提供四种检索模式并选择命名模型', async () => {
  renderComposer()
  fireEvent.click(screen.getByRole('button', { name: '混合检索' }))
  expect(screen.getByRole('button', { name: /引导检索/ })).toBeInTheDocument()
  expect(screen.queryByRole('button', { name: /精确检索/ })).not.toBeInTheDocument()
  fireEvent.click(screen.getByRole('button', { name: /模糊检索/ }))
  expect(screen.getByRole('button', { name: '模糊检索' })).toBeInTheDocument()
  expect(
    await screen.findByRole('option', { name: '我的对话模型（本地模拟）' }),
  ).toBeInTheDocument()
  fireEvent.change(screen.getByLabelText('对话模型'), { target: { value: '8' } })
  expect(screen.getByLabelText('对话模型')).toHaveValue('8')
})

it('格式与上传时间可以叠加过滤并在新检索时重置', () => {
  renderComposer()
  fireEvent.click(screen.getByRole('button', { name: '过滤条件' }))
  fireEvent.click(screen.getByLabelText('PDF'))
  fireEvent.change(screen.getByLabelText('上传起点'), { target: { value: '2026-10-01T10:00' } })
  fireEvent.change(screen.getByLabelText('上传终点'), { target: { value: '2026-10-24T18:00' } })
  expect(screen.getByLabelText('PDF')).toBeChecked()
  expect(screen.getByLabelText('上传起点')).toHaveValue('2026-10-01T10:00')
  fireEvent.click(screen.getByRole('button', { name: /新检索/ }))
  fireEvent.click(screen.getByRole('button', { name: '过滤条件' }))
  expect(screen.getByLabelText('PDF')).not.toBeChecked()
  expect(screen.getByLabelText('上传起点')).toHaveValue('')
})

it('勾选检索分块作为所选上下文，并能清空', async () => {
  useChat.setState({ conversationId: 9 })
  vi.mocked(request).mockImplementation(async (url) => {
    if (url.endsWith('/conversation/get'))
      return {
        code: 0,
        data: {
          id: 9,
          messages: [
            {
              id: 1,
              type: 'results',
              content: { results: [{ chunkId: 21, docTitle: '学习笔记', text: '向量检索内容' }] },
            },
          ],
        },
      } as never
    if (url.endsWith('/base/page')) return { code: 0, data: { list: [], total: 0 } } as never
    if (url.endsWith('/simple-list') || url.endsWith('/conversation/list'))
      return { code: 0, data: [] } as never
    throw new Error(`意外请求 ${url}`)
  })
  renderComposer()
  fireEvent.click(await screen.findByRole('checkbox', { name: '选择学习笔记分块1' }))
  expect(screen.getByText('所选上下文：1 个分块')).toBeInTheDocument()
  fireEvent.click(screen.getByRole('button', { name: '清空所选上下文' }))
  expect(screen.getByRole('checkbox', { name: '选择学习笔记分块1' })).not.toBeChecked()
})

it('模糊检索发送过滤快照，分页结果落库并展示真实命中信息', async () => {
  const messages: object[] = []
  useChat.setState({
    mode: '模糊检索',
    scope: ['7'],
    filters: { formats: [1], uploadedFrom: '2026-10-01T10:00' },
  })
  vi.mocked(request).mockImplementation(async (url, options) => {
    if (url.endsWith('/base/page'))
      return {
        code: 0,
        data: { list: [{ id: 7, name: '研究知识库', type: 1, ownerId: 1 }], total: 1 },
      } as never
    if (url.endsWith('/conversation/create')) return { code: 0, data: 9 } as never
    if (url.endsWith('/state/update')) return { code: 0, data: true } as never

    if (url.endsWith('/conversation/get')) return { code: 0, data: { id: 9, messages } } as never
    if (url.endsWith('/search') && (options?.data as API.DocSearchReqVO)?.pageNo === 2)
      return {
        code: 0,
        data: {
          list: [{ chunkId: 22, docTitle: '第二页记录', text: '后续结果', matchType: 'SEMANTIC' }],
          total: 23,
          indexingCount: 2,
        },
      } as never
    if (url.endsWith('/search')) {
      const body = options?.data as API.DocSearchReqVO
      const data = {
        list: [
          {
            chunkId: 21,
            docTitle: '实验记录',
            text: '神经网络知识',
            matchType: 'SEMANTIC',
            score: 0.85,
            ocr: true,
            pageNumber: 3,
          },
        ],
        total: 23,
        indexingCount: 2,
      }
      messages.push(
        { id: 1, type: 'user', content: { text: body.query, state: { ...body, mode: 'FUZZY' } } },
        {
          id: 2,
          type: 'results',
          content: { query: body.query, state: { ...body, mode: 'FUZZY' }, search: data },
        },
      )
      return { code: 0, data } as never
    }
    if (url.endsWith('/simple-list') || url.endsWith('/conversation/list'))
      return { code: 0, data: [] } as never
    throw new Error(`意外请求 ${url}`)
  })
  renderComposer()
  fireEvent.change(screen.getByLabelText('检索问题'), { target: { value: '神经网络' } })
  fireEvent.click(screen.getByRole('button', { name: '发送检索' }))
  await waitFor(() => expect(screen.getByText('实验记录')).toBeInTheDocument())
  expect(screen.getByText('语义命中')).toBeInTheDocument()
  expect(screen.getByText('OCR 文本')).toBeInTheDocument()
  expect(screen.getByText(/2 份文档正在入库/)).toBeInTheDocument()
  expect(request).toHaveBeenCalledWith(
    '/knowledge/search/search',
    expect.objectContaining({
      data: expect.objectContaining({
        mode: 'FUZZY',
        formats: [1],
        pageNo: 1,
        pageSize: 20,
        uploadedFrom: '2026-10-01T10:00:00',
      }),
    }),
  )
  fireEvent.click(screen.getByRole('button', { name: '下一页' }))
  expect(await screen.findByText('第二页记录')).toBeInTheDocument()
  expect(request).toHaveBeenCalledWith(
    '/knowledge/search/search',
    expect.objectContaining({
      data: expect.objectContaining({ mode: 'FUZZY', formats: [1], pageNo: 2 }),
    }),
  )
})

it('恢复会话自己的模式、模型、过滤和所选上下文，编辑后保存供下次恢复', async () => {
  let saved: API.RetrievalState = {
    mode: 'FUZZY',
    knowledgeBaseIds: [],
    formats: [6],
    modelId: 8,
    selectedChunkIds: [21],
  }
  vi.mocked(request).mockImplementation(async (url, options) => {
    if (url.endsWith('/conversation/list'))
      return { code: 0, data: [{ id: 9, title: '研究会话' }] } as never
    if (url.endsWith('/conversation/get'))
      return { code: 0, data: { id: 9, state: saved, messages: [] } } as never
    if (url.endsWith('/state/update')) {
      saved = (options?.data as API.ConversationStateReqVO).state
      return { code: 0, data: true } as never
    }
    if (url.endsWith('/simple-list'))
      return { code: 0, data: [{ id: 8, name: '我的对话模型', simulated: true }] } as never
    if (url.endsWith('/base/page')) return { code: 0, data: { list: [], total: 0 } } as never
    throw new Error(`意外请求 ${url}`)
  })
  renderComposer()
  fireEvent.click(await screen.findByRole('button', { name: '研究会话' }))
  expect(await screen.findByRole('button', { name: '模糊检索' })).toBeInTheDocument()
  expect(screen.getByLabelText('对话模型')).toHaveValue('8')
  expect(screen.getByText('所选上下文：1 个分块')).toBeInTheDocument()
  fireEvent.click(screen.getByRole('button', { name: '过滤条件' }))
  expect(screen.getByLabelText('Markdown')).toBeChecked()
  fireEvent.click(screen.getByLabelText('PDF'))
  await waitFor(() => expect(saved.formats).toEqual([6, 1]))
  fireEvent.click(screen.getByRole('button', { name: /新检索/ }))
  fireEvent.click(screen.getByRole('button', { name: '研究会话' }))
  expect(await screen.findByRole('button', { name: '模糊检索' })).toBeInTheDocument()
  fireEvent.click(screen.getByRole('button', { name: '过滤条件' }))
  expect(screen.getByLabelText('PDF')).toBeChecked()
})

it('引导推荐选项与自由输入保持原问题，可随时跳过', async () => {
  const messages: object[] = []
  let guideRound = 0
  let state: API.RetrievalState = {
    mode: 'GUIDED',
    knowledgeBaseIds: [7],
    modelId: 8,
    guideQuestion: '找学习材料',
    guideRound: 0,
  }
  useChat.setState({ mode: '引导检索', scope: ['7'], modelId: 8, guideState: {} })
  vi.mocked(request).mockImplementation(async (url, options) => {
    if (url.endsWith('/base/page'))
      return {
        code: 0,
        data: { list: [{ id: 7, name: '学习知识库', ownerId: 1, type: 1 }], total: 1 },
      } as never
    if (url.endsWith('/conversation/create')) return { code: 0, data: 9 } as never
    if (url.endsWith('/state/update')) {
      state = (options?.data as API.ConversationStateReqVO).state
      return { code: 0, data: true } as never
    }
    if (url.endsWith('/guide')) {
      const body = options?.data as API.DocGuideReqVO
      messages.push({
        id: messages.length + 1,
        type: 'user',
        content: { text: body.answer || body.query, state },
      })
      const response = body.skip
        ? {
            stage: 'RESULTS',
            round: guideRound,
            search: {
              list: [{ chunkId: 21, docTitle: '引导找到的笔记', text: '学习资料' }],
              total: 1,
            },
          }
        : {
            stage: 'CLARIFY',
            round: ++guideRound,
            question: guideRound === 1 ? '想看哪方面？' : '希望什么格式？',
            options: ['机器学习', '知识检索'],
          }
      state = {
        ...state,
        guideQuestion: body.query,
        guideRound,
        mode: body.skip ? 'HYBRID' : 'GUIDED',
      }
      messages.push({
        id: messages.length + 1,
        type: body.skip ? 'results' : 'guide',
        content: response,
      })
      return { code: 0, data: response } as never
    }
    if (url.endsWith('/conversation/get'))
      return { code: 0, data: { id: 9, state, messages } } as never
    if (url.endsWith('/simple-list') || url.endsWith('/conversation/list'))
      return { code: 0, data: [] } as never
    throw new Error(`意外请求 ${url}`)
  })
  renderComposer()
  fireEvent.change(screen.getByLabelText('检索问题'), { target: { value: '找学习材料' } })
  fireEvent.click(screen.getByRole('button', { name: '发送检索' }))
  expect(await screen.findByText('想看哪方面？')).toBeInTheDocument()
  fireEvent.click(screen.getByRole('button', { name: '机器学习' }))
  expect(await screen.findByText('希望什么格式？')).toBeInTheDocument()
  fireEvent.click(screen.getByRole('button', { name: '跳过引导，直接混合检索' }))
  await waitFor(() => expect(screen.getByText('引导找到的笔记')).toBeInTheDocument())
  expect(request).toHaveBeenCalledWith(
    '/knowledge/search/guide',
    expect.objectContaining({
      data: expect.objectContaining({ query: '找学习材料', skip: true, conversationId: 9 }),
    }),
  )
})

it('RAG 使用所选上下文，最终拒答完整替换暂定答案和引用', async () => {
  let packets: ReadableStreamDefaultController<Uint8Array> | undefined
  let messages: object[] = []
  useChat.setState({ mode: 'RAG 检索', selectedChunkIds: [21], modelId: 8 })
  vi.mocked(request).mockImplementation(async (url) => {
    if (url.endsWith('/base/page')) return { code: 0, data: { list: [], total: 0 } } as never
    if (url.endsWith('/conversation/create')) return { code: 0, data: 9 } as never
    if (url.endsWith('/state/update')) return { code: 0, data: true } as never
    if (url.endsWith('/conversation/get')) return { code: 0, data: { id: 9, messages } } as never
    if (url.endsWith('/simple-list') || url.endsWith('/conversation/list'))
      return { code: 0, data: [] } as never
    if (url.endsWith('/ask-stream'))
      return new ReadableStream<Uint8Array>({
        start(controller) {
          packets = controller
        },
      }) as never
    throw new Error(`意外请求 ${url}`)
  })
  renderComposer()
  fireEvent.change(screen.getByLabelText('检索问题'), { target: { value: '解释实验结果' } })
  fireEvent.click(screen.getByRole('button', { name: '发送检索' }))
  await waitFor(() => expect(packets).toBeDefined())
  const emit = (event: string, data: unknown) =>
    packets!.enqueue(
      new TextEncoder().encode(`event: ${event}\ndata: ${JSON.stringify({ code: 0, data })}\n\n`),
    )
  emit('delta', '暂定结论[1]')
  emit('citations', [{ number: 1, chunkId: 21, docTitle: '已撤回证据' }])
  expect(await screen.findByText('生成中 · 暂定内容')).toBeInTheDocument()
  expect(screen.getByText(/暂定结论/)).toBeInTheDocument()
  const final = {
    answered: false,
    answer: '依据已失效，请重新选择上下文。',
    citations: [],
    suggestions: ['重新检索'],
  }
  messages = [
    { id: 1, type: 'user', content: { text: '解释实验结果', mode: 'RAG 检索' } },
    { id: 2, type: 'answer', content: final },
  ]
  emit('done', final)
  packets!.close()
  await waitFor(() =>
    expect(screen.getByText('依据已失效，请重新选择上下文。')).toBeInTheDocument(),
  )
  expect(screen.queryByText(/暂定结论/)).not.toBeInTheDocument()
  expect(screen.queryByText('已撤回证据')).not.toBeInTheDocument()
  expect(request).toHaveBeenCalledWith(
    '/knowledge/rag/ask-stream',
    expect.objectContaining({
      data: expect.objectContaining({ modelId: 8, selectedChunkIds: [21] }),
    }),
  )
})
