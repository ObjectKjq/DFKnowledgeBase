import { updateState } from '@/api/conversationController'
import { useAuthStore } from '@/stores/auth'
import { useChat, type ChatMode } from '@/stores/chat'

export const MODE_CODES: Record<ChatMode, string> = {
  混合检索: 'HYBRID',
  模糊检索: 'FUZZY',
  引导检索: 'GUIDED',
  'RAG 检索': 'RAG',
}
export function modeLabel(mode?: string): ChatMode {
  return (
    (Object.entries(MODE_CODES).find(([, code]) => code === mode)?.[0] as ChatMode) ?? '混合检索'
  )
}
const timestamp = (value?: string) => (value && value.length === 16 ? `${value}:00` : value)

/** 在发送和持久化边界创建独立选择快照，后续编辑不改变历史轮次。 */
export function currentSelection(): API.RetrievalState {
  const state = useChat.getState()
  return {
    mode: MODE_CODES[state.mode],
    knowledgeBaseIds: state.scope.map(Number),
    modelId: state.modelId,
    formats: [...state.filters.formats],
    uploadedFrom: timestamp(state.filters.uploadedFrom),
    uploadedTo: timestamp(state.filters.uploadedTo),
    selectedChunkIds: [...state.selectedChunkIds],
    ...state.guideState,
  }
}

let saving: Promise<unknown> = Promise.resolve()
/** 会话选择按序保存，防止较早请求覆盖较晚选择。 */
export function saveSelection(id: number, state: API.RetrievalState) {
  const token = useAuthStore.getState().token
  const next = saving
    .catch(() => undefined)
    .then(() => {
      if (token !== useAuthStore.getState().token) throw new Error('登录身份已变更')
      return updateState({ id, state }, { silent: true })
    })
  saving = next
  return next
}
