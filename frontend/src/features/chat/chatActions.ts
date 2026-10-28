import { queryClient } from '@/lib/queryClient'
import { useAuthStore } from '@/stores/auth'
import { useChat, type PendingTurn } from '@/stores/chat'

import {
  conversationKey,
  conversationsKey,
  normalizeMessages,
  type ChatMessage,
  type Conversation,
} from './conversations'
import { streamConversation } from './conversationStream'
import { currentSelection, modeLabel, selectionKey, waitForSelectionSave } from './selection'

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

/** 根据同一次发送的响应更新侧栏摘要，不重新查询整份列表。 */
function rememberConversation(token: string, summary: API.ConversationRespVO) {
  if (!summary?.id) throw new Error('会话响应缺少编号')
  queryClient.setQueryData<Conversation>(conversationKey(token, summary.id), (previous) => ({
    ...previous,
    ...summary,
    messages: previous?.messages ?? [],
  }))
  queryClient.setQueryData<API.ConversationRespVO[]>(conversationsKey(token), (previous) => [
    summary,
    ...(previous ?? []).filter((item) => item.id !== summary.id),
  ])
}

/** 一次请求完成会话创建、选择保存和内容接收，结果直接合并到服务端状态缓存。 */
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
  const previous = queryClient.getQueryData<Conversation>(
    conversationKey(token, state.conversationId),
  )
  const pending: PendingTurn = {
    conversationId: state.conversationId,
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
      useChat.setState((current) => ({
        pending: current.pending ? { ...current.pending, ...values } : null,
      }))
  }
  try {
    // 只等待用户此前主动修改选择的保存，避免旧请求晚于本轮完整快照落库。
    await waitForSelectionSave()
    request.signal.throwIfAborted()
    if (!active()) return
    await streamConversation(
      {
        conversationId: state.conversationId ?? undefined,
        question,
        state: pending.selection!,
        afterMessageId: pending.previousMessageId,
        guideQuestion: pending.guideOriginalQuestion,
        guideAnswer: pending.guideAnswer,
        skipGuide: pending.skipGuide,
      },
      request.signal,
      ({ event, data }) => {
        if (!active()) return
        if (event === 'conversation') {
          const summary = data as API.ConversationRespVO
          rememberConversation(token, summary)
          // 先写入缓存，再切换当前会话，避免新组件挂载时补发详情请求。
          useChat.setState((current) => ({
            conversationId: summary.id!,
            pending: current.pending ? { ...current.pending, conversationId: summary.id! } : null,
          }))
        } else if (event === 'delta') {
          if (typeof data !== 'string') throw new Error('问答增量格式错误')
          update({ text: (useChat.getState().pending?.text ?? '') + data })
        } else if (event === 'citations') {
          if (!Array.isArray(data)) throw new Error('问答引用格式错误')
          update({ citations: data as API.RagCitationRespVO[] })
        } else if (event === 'done') {
          const turn = data as API.ConversationTurnRespVO
          if (!turn?.conversation?.id || !Array.isArray(turn.messages))
            throw new Error('会话结果格式错误')
          const summary = turn.conversation
          const fresh = normalizeMessages(turn.messages as unknown as ChatMessage[])
          if (fresh.some((message) => !message.id || !message.type))
            throw new Error('会话消息格式错误')
          const selectionUnchanged =
            selectionKey(currentSelection()) === selectionKey(pending.selection!)
          rememberConversation(token, summary)
          queryClient.setQueryData<Conversation>(conversationKey(token, summary.id!), (cached) => {
            const messages = new Map(
              (cached?.messages ?? []).map((message) => [message.id, message]),
            )
            fresh.forEach((message) => messages.set(message.id, message))
            return {
              ...cached,
              ...summary,
              messages: [...messages.values()].sort((a, b) => (a.id ?? 0) - (b.id ?? 0)),
            }
          })
          const saved = summary.state
          const last = fresh.at(-1)
          useChat.setState({
            conversationId: summary.id!,
            pending: null,
            ...(selectionUnchanged && saved
              ? {
                  mode: modeLabel(saved.mode),
                  guideState: {
                    guideQuestion: saved.guideQuestion,
                    guideRound: saved.guideRound,
                    guideAnswers: saved.guideAnswers,
                  },
                  guidePrompt:
                    last?.type === 'guide' ? (last.content as API.DocGuideRespVO) : undefined,
                }
              : {}),
          })
        }
      },
    )
  } catch (error) {
    update({
      text: '',
      citations: [],
      error: request.signal.aborted
        ? '生成已停止，可重新提问。'
        : error instanceof Error
          ? error.message
          : '提问失败，请重试',
    })
  } finally {
    if (active()) {
      useChat.setState({ busy: false })
      controller = null
    }
  }
}
