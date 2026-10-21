import { useQuery } from '@tanstack/react-query'
import { useEffect } from 'react'

import { getConversation, getConversationList } from '@/api/conversationController'
import { useAuthStore } from '@/stores/auth'
import { useChat } from '@/stores/chat'

export interface MessageContent extends API.RagAnswerRespVO {
  text?: string
  question?: string
  mode?: string
  knowledgeBaseIds?: number[]
  results?: API.DocChunkRespVO[]
}
export type ChatMessage = Omit<API.MessageRespVO, 'content'> & { content: MessageContent }
export type Conversation = Omit<API.ConversationDetailRespVO, 'messages'> & {
  messages: ChatMessage[]
}

export const conversationKey = (token: string | null, id: number | null) => [
  'chat-conversation',
  token,
  id,
]
export const conversationsKey = (token: string | null) => ['chat-conversations', token]

/** JsonNode 在生成契约中是不透明类型，在消息边界解释为后端结构化内容。 */
export async function fetchConversation(id: number, signal?: AbortSignal): Promise<Conversation> {
  const { data } = await getConversation({ id }, { signal, silent: true })
  if (!data) throw new Error('会话不存在或已删除')
  const messages = (data.messages ?? []) as unknown as ChatMessage[]
  return {
    ...data,
    messages: messages
      .map((message) => ({
        ...message,
        content: message.content && typeof message.content === 'object' ? message.content : {},
      }))
      .sort(
        (a, b) =>
          (a.createTime ?? '').localeCompare(b.createTime ?? '') || (a.id ?? 0) - (b.id ?? 0),
      ),
  }
}

/** 侧栏会话列表仅来自当前登录用户。 */
export function useConversations() {
  const token = useAuthStore((s) => s.token)
  return useQuery({
    queryKey: conversationsKey(token),
    queryFn: async () => (await getConversationList()).data ?? [],
    enabled: !!token,
  })
}

/** 恢复服务器消息；首次打开时恢复最近一次提问的范围与模式。 */
export function useConversation() {
  const token = useAuthStore((s) => s.token)
  const id = useChat((s) => s.conversationId)
  const restoreScope = useChat((s) => s.restoreScope)
  const query = useQuery({
    queryKey: conversationKey(token, id),
    queryFn: ({ signal }) => fetchConversation(id!, signal),
    enabled: !!token && id !== null,
  })
  useEffect(() => {
    if (!restoreScope || !query.data || query.isFetching || query.isError) return
    const lastUser = [...query.data.messages].reverse().find((message) => message.type === 'user')
    useChat.setState({
      scope: (lastUser?.content.knowledgeBaseIds ?? query.data.knowledgeBaseIds ?? []).map(String),
      mode: lastUser?.content.mode === 'RAG 检索' ? 'RAG 检索' : '混合检索',
      restoreScope: false,
    })
  }, [query.data, query.isFetching, query.isError, restoreScope])
  return query
}
