import { createConversation } from '@/api/conversationController'
import { guide, search } from '@/api/docSearchController'
import { queryClient } from '@/lib/queryClient'
import { useAuthStore } from '@/stores/auth'
import { useChat, type PendingTurn } from '@/stores/chat'

import {
  conversationKey,
  conversationsKey,
  fetchConversation,
  type Conversation,
} from './conversations'
import { streamRag } from './ragStream'
import { currentSelection, saveSelection } from './selection'

let controller: AbortController | null = null

/** 用户主动停止或退出登录时中断流；不保留未完成的暂定答案。 */
export function stopQuestion() {
  controller?.abort()
}

useAuthStore.subscribe((state, previous) => {
  if (state.token === previous.token) return
  stopQuestion()
  useChat.setState({
    conversationId: null,
    scope: [],
    restoreScope: false,
    draft: '',
    busy: false,
    pending: null,
    previewChunkId: null,
    mode: '混合检索',
    modelId: undefined,
    filters: { formats: [] },
    selectedChunkIds: [],
    guideState: {},
    guidePrompt: undefined,
  })
})

/** 发送一个真实提问；范围和模式在点击发送时固定，不影响随后编辑的选择。 */
export async function sendQuestion(
  override?: string,
  options?: { skipGuide?: boolean; retry?: PendingTurn },
) {
  const state = useChat.getState()
  const question = (options?.retry?.question ?? override ?? state.draft).trim()
  const token = useAuthStore.getState().token
  if (!question || state.busy || !token || question.length > 2000 || state.restoreScope) return
  const request = new AbortController()
  controller = request
  let id = state.conversationId
  const previous = queryClient.getQueryData<Conversation>(conversationKey(token, id))
  const pending: PendingTurn = {
    conversationId: id,
    previousMessageId: Math.max(0, ...(previous?.messages ?? []).map((message) => message.id ?? 0)),
    question,
    knowledgeBaseIds: options?.retry?.knowledgeBaseIds ?? state.scope.map(Number),
    mode: options?.retry?.mode ?? state.mode,
    text: '',
    citations: [],
    selection: options?.retry?.selection ?? currentSelection(),
    guideOriginalQuestion:
      options?.retry?.guideOriginalQuestion ??
      (state.guidePrompt?.stage === 'CLARIFY' ? state.guideState.guideQuestion : question),
    guideAnswer:
      options?.retry?.guideAnswer ??
      (state.guidePrompt?.stage === 'CLARIFY' && !options?.skipGuide ? question : undefined),
    skipGuide: options?.retry?.skipGuide ?? options?.skipGuide,
  }
  useChat.setState({ busy: true, draft: '', pending })
  const active = () => controller === request && token === useAuthStore.getState().token
  const update = (values: Partial<PendingTurn>) => {
    if (active())
      useChat.setState((state) => ({
        pending: state.pending ? { ...state.pending, ...values } : null,
      }))
  }
  let succeeded = false
  try {
    if (id === null) {
      const { data } = await createConversation(
        { title: question.slice(0, 80), knowledgeBaseIds: pending.knowledgeBaseIds },
        { signal: request.signal, silent: true },
      )
      if (!data) throw new Error('会话创建失败，请重试')
      id = data
      if (!active()) return
      useChat.setState({ conversationId: id })
      update({ conversationId: id })
      queryClient.setQueryData<Conversation>(conversationKey(token, id), {
        id,
        title: question.slice(0, 80),
        knowledgeBaseIds: pending.knowledgeBaseIds,
        messages: [],
      })
      void queryClient.invalidateQueries({ queryKey: conversationsKey(token) })
    }
    await saveSelection(id, pending.selection!)
    if (pending.mode === '引导检索') {
      const { data } = await guide(
        {
          ...pending.selection,
          mode: 'HYBRID',
          conversationId: id,
          query: pending.guideOriginalQuestion ?? question,
          answer: pending.guideAnswer,
          skip: pending.skipGuide,
          pageNo: 1,
          pageSize: 20,
        },
        { signal: request.signal, silent: true },
      )
      if (!data) throw new Error('引导响应不可用，请重试')
      if (active())
        useChat.setState({
          guidePrompt: data.stage === 'CLARIFY' ? data : undefined,
          ...(data.stage !== 'CLARIFY' && useChat.getState().mode === pending.mode
            ? { mode: '混合检索' }
            : {}),
        })
      if (data.search) update({ results: data.search.list ?? [], searchPage: data.search })
    } else if (pending.mode === 'RAG 检索') {
      // 对本次提问附加范围；后端未传该字段的旧调用仍使用会话初始范围。
      const body = { conversationId: id, question, ...pending.selection }
      await streamRag(body, request.signal, ({ event, data }) => {
        if (!active()) return
        if (event === 'delta') {
          if (typeof data !== 'string') throw new Error('问答增量格式错误')
          update({ text: (useChat.getState().pending?.text ?? '') + data })
        } else if (event === 'citations') {
          if (!Array.isArray(data)) throw new Error('问答引用格式错误')
          update({ citations: data as API.RagCitationRespVO[] })
        } else if (event === 'done') {
          const result = data as API.RagAnswerRespVO
          if (!result || typeof result.answer !== 'string' || typeof result.answered !== 'boolean')
            throw new Error('问答结果格式错误')
          // 最终校验可能拒答，必须完整替换暂定正文和引用。
          update({
            result,
            text: result.answer,
            citations: result.answered ? (result.citations ?? []) : [],
          })
        }
      })
    } else {
      const { data } = await search(
        {
          conversationId: id,
          query: question,
          ...pending.selection,
          mode: pending.mode === '模糊检索' ? 'FUZZY' : 'HYBRID',
          pageNo: 1,
          pageSize: 20,
        },
        { signal: request.signal, silent: true },
      )
      const results = data?.list ?? []
      update({ results, searchPage: data })
    }
    succeeded = true
    update({ completed: true })
  } catch (error) {
    update({
      text: '',
      citations: [],
      result: undefined,
      error: request.signal.aborted
        ? '生成已停止，可重新提问。'
        : error instanceof Error
          ? error.message
          : '提问失败，请重试',
    })
  } finally {
    if (id !== null && active()) {
      try {
        // 先取消更早发起的查询，避免把提问前的快照当作完成后的历史。
        await queryClient.cancelQueries({ queryKey: conversationKey(token, id) })
        const conversation = await queryClient.fetchQuery({
          queryKey: conversationKey(token, id),
          queryFn: ({ signal }) => fetchConversation(id!, signal),
          staleTime: 0,
        })
        if (active()) {
          queryClient.setQueryData(conversationKey(token, id), conversation)
          if (conversation.state)
            useChat.setState({
              guideState: {
                guideQuestion: conversation.state.guideQuestion,
                guideRound: conversation.state.guideRound,
                guideAnswers: conversation.state.guideAnswers,
              },
            })
          if (succeeded) useChat.setState({ pending: null })
        }
      } catch (error) {
        if (succeeded)
          update({
            error: `回答已完成，但历史记录刷新失败：${error instanceof Error ? error.message : '请重新打开会话'}`,
          })
      }
    }
    if (active()) {
      useChat.setState({ busy: false })
      void queryClient.invalidateQueries({ queryKey: conversationsKey(token) })
      controller = null
    }
  }
}
