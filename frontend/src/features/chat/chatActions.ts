import { createConversation, createMessage } from '@/api/conversationController'
import { search } from '@/api/docSearchController'
import { queryClient } from '@/lib/queryClient'
import { useAuthStore } from '@/stores/auth'
import { useChat, type PendingTurn } from '@/stores/chat'

import {
  conversationKey,
  conversationsKey,
  fetchConversation,
  type Conversation,
  type MessageContent,
} from './conversations'
import { streamRag } from './ragStream'

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
  })
})

/** 发送一个真实提问；范围和模式在点击发送时固定，不影响随后编辑的选择。 */
export async function sendQuestion(override?: string) {
  const state = useChat.getState()
  const question = (override ?? state.draft).trim()
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
    knowledgeBaseIds: state.scope.map(Number),
    mode: state.mode,
    text: '',
    citations: [],
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
    if (pending.mode === 'RAG 检索') {
      // 对本次提问附加范围；后端未传该字段的旧调用仍使用会话初始范围。
      const body = { conversationId: id, question, knowledgeBaseIds: pending.knowledgeBaseIds }
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
      const save = (type: API.MessageCreateReqVO['type'], content: MessageContent) =>
        createMessage(
          { conversationId: id!, type, content: content as unknown as API.JsonNode },
          { signal: request.signal, silent: true },
        )
      await save('user', {
        text: question,
        mode: '混合检索',
        knowledgeBaseIds: pending.knowledgeBaseIds,
      })
      const { data } = await search(
        { query: question, knowledgeBaseIds: pending.knowledgeBaseIds },
        { signal: request.signal, silent: true },
      )
      await save(data?.length ? 'results' : 'empty', {
        question,
        results: data ?? [],
        answer: data?.length
          ? undefined
          : '当前范围内未找到相关资料，请调整范围、换一种问法或补充文档。',
      })
      update({ results: data ?? [] })
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
