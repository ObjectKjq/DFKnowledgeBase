import { updateState } from '@/api/conversationController'
import { useAuthStore } from '@/stores/auth'
import { useChat, type ChatMode } from '@/stores/chat'
import { queryClient } from '@/lib/queryClient'
import type { Conversation } from './conversations'
import { conversationKey } from './conversationKeys'

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

/** 归一化缺省值后比较选择，发送完成或组件挂载不触发重复保存。 */
export function selectionKey(state: API.RetrievalState): string {
  return JSON.stringify({
    mode: state.mode ?? 'HYBRID',
    knowledgeBaseIds: state.knowledgeBaseIds ?? [],
    modelId: state.modelId ?? undefined,
    formats: state.formats ?? [],
    uploadedFrom: timestamp(state.uploadedFrom) || undefined,
    uploadedTo: timestamp(state.uploadedTo) || undefined,
    selectedChunkIds: state.selectedChunkIds ?? [],
    guideQuestion: state.guideQuestion || undefined,
    guideRound: state.guideRound ?? 0,
    guideAnswers: state.guideAnswers ?? [],
  })
}

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

/** 等待已发起的选择修改，发送本身不再额外保存一次。 */
export function waitForSelectionSave() {
  return saving.catch(() => undefined)
}
/** 会话选择按序保存，防止较早请求覆盖较晚选择。 */
export function saveSelection(id: number, state: API.RetrievalState) {
  const token = useAuthStore.getState().token
  const next = saving
    .catch(() => undefined)
    .then(async () => {
      if (token !== useAuthStore.getState().token) throw new Error('登录身份已变更')
      const result = await updateState({ id, state }, { silent: true })
      if (token === useAuthStore.getState().token)
        queryClient.setQueryData<Conversation>(conversationKey(token, id), (previous) =>
          previous ? { ...previous, state } : previous,
        )
      return result
    })
  saving = next
  return next
}
