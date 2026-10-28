import { useQuery } from '@tanstack/react-query'
import { useEffect } from 'react'

import { getConversation, getConversationList } from '@/api/conversationController'
import { useAuthStore } from '@/stores/auth'
import { useChat } from '@/stores/chat'
import { modeLabel } from './selection'
import { conversationKey, conversationsKey } from './conversationKeys'

export { conversationKey, conversationsKey } from './conversationKeys'

export interface MessageContent extends API.RagAnswerRespVO {
  text?: string
  question?: string
  query?: string
  originalQuestion?: string
  mode?: string
  knowledgeBaseIds?: number[]
  results?: API.DocChunkRespVO[]
  selection?: API.RetrievalState
  searchPage?: API.DocSearchPageRespVO
  pageNo?: number
  state?: API.RetrievalState
  search?: API.DocSearchPageRespVO
  options?: string[]
  round?: number
  reason?: string
  stage?: string
}
export type ChatMessage = Omit<API.MessageRespVO, 'content'> & { content: MessageContent }
export type Conversation = Omit<API.ConversationDetailRespVO, 'messages'> & {
  messages: ChatMessage[]
}

/** JsonNode 在生成契约中是不透明类型，在消息边界解释为后端结构化内容。 */
export async function fetchConversation(id: number, signal?: AbortSignal): Promise<Conversation> {
  const { data } = await getConversation({ id }, { signal, silent: true })
  if (!data) throw new Error('会话不存在或已删除')
  const messages = (data.messages ?? []) as unknown as ChatMessage[]
  return {
    ...data,
    messages: normalizeMessages(messages),
  }
}

/** 消息列表查询与发送响应共用同一结构化内容边界。 */
export function normalizeMessages(messages: ChatMessage[]): ChatMessage[] {
  return messages
    .map((message) => ({
      ...message,
      content: normalizeContent(message.content),
    }))
    .sort(
      (a, b) => (a.createTime ?? '').localeCompare(b.createTime ?? '') || (a.id ?? 0) - (b.id ?? 0),
    )
}

function normalizeContent(content: MessageContent): MessageContent {
  if (!content || typeof content !== 'object') return {}
  const selection = content.selection ?? content.state
  return {
    ...content,
    question: content.query ?? content.question,
    selection,
    mode: content.mode ?? (selection ? modeLabel(selection.mode) : undefined),
    results: content.results ?? content.search?.list,
    searchPage: content.searchPage ?? content.search,
  }
}

/** 侧栏会话列表仅来自当前登录用户。 */
export function useConversations() {
  const token = useAuthStore((s) => s.token)
  return useQuery({
    queryKey: conversationsKey(token),
    queryFn: async () => (await getConversationList()).data ?? [],
    enabled: !!token,
    staleTime: Infinity,
  })
}

/** 恢复服务器消息；首次打开时恢复最近一次提问的范围与模式。 */
export function useConversation() {
  const token = useAuthStore((s) => s.token)
  const id = useChat((s) => s.conversationId)
  const restoreScope = useChat((s) => s.restoreScope)
  const busy = useChat((s) => s.busy)
  const query = useQuery({
    queryKey: conversationKey(token, id),
    queryFn: ({ signal }) => fetchConversation(id!, signal),
    enabled: !!token && id !== null && !busy,
    staleTime: restoreScope ? 0 : Infinity,
  })
  useEffect(() => {
    if (!restoreScope || !query.data || query.isFetching || query.isError) return
    const lastUser = [...query.data.messages].reverse().find((message) => message.type === 'user')
    const saved = query.data.state ?? lastUser?.content.selection
    useChat.setState({
      scope: (
        saved?.knowledgeBaseIds ??
        lastUser?.content.knowledgeBaseIds ??
        query.data.knowledgeBaseIds ??
        []
      ).map(String),
      mode: saved
        ? modeLabel(saved.mode)
        : lastUser?.content.mode === 'RAG 检索'
          ? 'RAG 检索'
          : '混合检索',
      modelId: saved?.modelId,
      filters: {
        formats: saved?.formats ?? [],
        uploadedFrom: saved?.uploadedFrom,
        uploadedTo: saved?.uploadedTo,
      },
      selectedChunkIds: saved?.selectedChunkIds ?? [],
      guideState: {
        guideQuestion: saved?.guideQuestion,
        guideRound: saved?.guideRound,
        guideAnswers: saved?.guideAnswers,
      },
      guidePrompt:
        query.data.messages.at(-1)?.type === 'guide'
          ? (query.data.messages.at(-1)?.content as API.DocGuideRespVO)
          : undefined,
      restoreScope: false,
    })
  }, [query.data, query.isFetching, query.isError, restoreScope])
  return query
}
