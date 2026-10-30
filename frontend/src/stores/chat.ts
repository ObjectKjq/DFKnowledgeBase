import { create } from 'zustand'

export type ChatMode = '混合检索' | '关键字检索' | '引导检索' | 'RAG 检索'

export interface ChatFilters {
  formats: number[]
  uploadedFrom?: string
  uploadedTo?: string
}

export interface PendingTurn {
  conversationId: number | null
  previousMessageId: number
  question: string
  knowledgeBaseIds: number[]
  mode: ChatMode
  modelId?: number
  text: string
  citations: API.RagCitationRespVO[]
  selection?: API.RetrievalState
  error?: string
  guideOriginalQuestion?: string
  guideAnswer?: string
  skipGuide?: boolean
}

interface ChatState {
  conversationId: number | null
  scope: string[]
  restoreScope: boolean
  mode: ChatMode
  modelId?: number
  filters: ChatFilters
  selectedChunkIds: number[]
  guideState: Pick<API.RetrievalState, 'guideQuestion' | 'guideRound' | 'guideAnswers'>
  guidePrompt?: API.DocGuideRespVO
  draft: string
  busy: boolean
  pending: PendingTurn | null
  previewChunkId: number | null
  newChat: () => void
  selectConversation: (id: number) => void
  setScope: (scope: string[]) => void
  setDraft: (draft: string) => void
  setMode: (mode: ChatMode) => void
  setModelId: (modelId?: number) => void
  setFilters: (filters: ChatFilters) => void
  toggleChunk: (id: number) => void
  clearChunks: () => void
  showPreview: (chunkId: number) => void
  closePreview: () => void
}

/** 仅保存聊天的客户端选择和流式暂定内容，服务端会话不写入本地存储。 */
export const useChat = create<ChatState>()((set, get) => ({
  conversationId: null,
  scope: [],
  restoreScope: false,
  mode: '混合检索',
  filters: { formats: [] },
  selectedChunkIds: [],
  guideState: {},
  draft: '',
  busy: false,
  pending: null,
  previewChunkId: null,
  newChat: () => {
    if (get().busy) return
    set({
      conversationId: null,
      scope: [],
      mode: '混合检索',
      modelId: undefined,
      filters: { formats: [] },
      selectedChunkIds: [],
      guideState: {},
      guidePrompt: undefined,
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
  setModelId: (modelId) => set({ modelId }),
  setFilters: (filters) => set({ filters }),
  toggleChunk: (id) =>
    set((state) => ({
      selectedChunkIds: state.selectedChunkIds.includes(id)
        ? state.selectedChunkIds.filter((value) => value !== id)
        : [...state.selectedChunkIds, id],
    })),
  clearChunks: () => set({ selectedChunkIds: [] }),
  showPreview: (chunkId) => set({ previewChunkId: chunkId }),
  closePreview: () => set({ previewChunkId: null }),
}))
