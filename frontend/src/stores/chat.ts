import { create } from 'zustand'

export type ChatMode = '混合检索' | 'RAG 检索'

export interface PendingTurn {
  conversationId: number | null
  previousMessageId: number
  question: string
  knowledgeBaseIds: number[]
  mode: ChatMode
  text: string
  citations: API.RagCitationRespVO[]
  result?: API.RagAnswerRespVO
  results?: API.DocChunkRespVO[]
  completed?: boolean
  error?: string
}

interface ChatState {
  conversationId: number | null
  scope: string[]
  restoreScope: boolean
  mode: ChatMode
  draft: string
  busy: boolean
  pending: PendingTurn | null
  previewChunkId: number | null
  newChat: () => void
  selectConversation: (id: number) => void
  setScope: (scope: string[]) => void
  setDraft: (draft: string) => void
  setMode: (mode: ChatMode) => void
  showPreview: (chunkId: number) => void
  closePreview: () => void
}

/** 仅保存聊天的客户端选择和流式暂定内容，服务端会话不写入本地存储。 */
export const useChat = create<ChatState>()((set, get) => ({
  conversationId: null,
  scope: [],
  restoreScope: false,
  mode: '混合检索',
  draft: '',
  busy: false,
  pending: null,
  previewChunkId: null,
  newChat: () => {
    if (get().busy) return
    set({
      conversationId: null,
      draft: '',
      pending: null,
      previewChunkId: null,
      restoreScope: false,
    })
  },
  selectConversation: (id) => {
    if (get().busy) return
    set({ conversationId: id, draft: '', pending: null, previewChunkId: null, restoreScope: true })
  },
  setScope: (scope) => set({ scope, restoreScope: false }),
  setDraft: (draft) => set({ draft }),
  setMode: (mode) => set({ mode }),
  showPreview: (chunkId) => set({ previewChunkId: chunkId }),
  closePreview: () => set({ previewChunkId: null }),
}))
