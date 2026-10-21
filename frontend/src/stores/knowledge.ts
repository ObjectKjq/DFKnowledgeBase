/** 管理页尚未接入的模型/MCP 演示状态，以及共享弹层、提示和临时上传队列。 */
import { create } from 'zustand'
import { persist } from 'zustand/middleware'

import { documentUploadError } from '@/features/documents/documents'
import { getLibraries } from '@/features/knowledge/libraries'
import {
  DEFAULT_CREDENTIALS,
  DEFAULT_MODEL_CONFIG,
  MCP_TOOLS,
  STORE_KEY,
  type Credential,
  type ModelConfig,
} from '@/mock/data'
import { configText, uuid } from '@/mock/logic'
import { useChat } from '@/stores/chat'

export type ModalState =
  | { kind: 'upload' }
  | { kind: 'credential' }
  | { kind: 'token'; token: string }
  | { kind: 'library' }
  | { kind: 'vector-confirm' }
  | { kind: 'copy'; text: string }
  | { kind: 'confirm'; title: string; text: string; confirmLabel: string }

interface KbState {
  credentials: Credential[]
  modelConfig: ModelConfig
  popover: string
  modal: ModalState | null
  modalConfirm: (() => void) | null
  toast: { msg: string; error: boolean } | null
  lastConfigClient: string
  pendingUpload: File[]
  uploadTarget: string
  uploadStarted: boolean
  oneTimeToken: string | null
  toastMsg: (msg: string, error?: boolean) => void
  clearToast: () => void
  openPopover: (name: string) => void
  closePopover: () => void
  openModal: (modal: ModalState, confirm?: () => void) => void
  closeModal: () => void
  createCredential: (name: string, days: number) => void
  revokeCredential: (id: string) => void
  openUpload: () => void
  addPendingFiles: (files: File[]) => void
  removePending: (index: number) => void
  setUploadTarget: (id: string) => void
  setUploadStarted: (started: boolean) => void
  saveModelConfig: (
    kind: keyof ModelConfig,
    cfg: { name: string; url: string; type: string },
  ) => void
  setLastConfigClient: (client: string) => void
  copyText: (text: string) => Promise<void>
  resetDemo: () => void
  confirmThen: (title: string, text: string, confirmLabel: string, fn: () => void) => void
}

let toastTimer: ReturnType<typeof setTimeout> | undefined
export const useKb = create<KbState>()(
  persist(
    (set, get) => ({
      credentials: structuredClone(DEFAULT_CREDENTIALS),
      modelConfig: structuredClone(DEFAULT_MODEL_CONFIG),
      popover: '',
      modal: null,
      modalConfirm: null,
      toast: null,
      lastConfigClient: 'Codex',
      pendingUpload: [],
      uploadTarget: '',
      uploadStarted: false,
      oneTimeToken: null,
      toastMsg: (msg, error = false) => {
        set({ toast: { msg, error } })
        clearTimeout(toastTimer)
        toastTimer = setTimeout(() => set({ toast: null }), 4200)
      },
      clearToast: () => set({ toast: null }),
      openPopover: (name) => set({ popover: get().popover === name ? '' : name }),
      closePopover: () => set({ popover: '' }),
      openModal: (m, confirm) => set({ modal: m, modalConfirm: confirm ?? null }),
      closeModal: () => {
        if (get().uploadStarted) {
          get().toastMsg('正在上传文档，请稍候')
          return
        }
        set({ modal: null, modalConfirm: null, oneTimeToken: null, pendingUpload: [] })
      },

      createCredential: (name, days) => {
        const date = new Date('2026-10-16T12:00:00+08:00')
        date.setDate(date.getDate() + days)
        const cred: Credential = {
          id: uuid(),
          name,
          expires: date.toISOString().slice(0, 10),
          status: '有效',
          last: '尚未使用',
          tools: [...MCP_TOOLS],
        }
        const token = 'DEMO_ONLY_' + uuid() + '_' + uuid()
        set({
          credentials: [...get().credentials, cred],
          oneTimeToken: token,
          modal: { kind: 'token', token },
        })
      },
      revokeCredential: (id) => {
        get().confirmThen(
          '撤销凭证',
          '撤销后，该凭证将立即无法继续调用。此操作不可恢复。',
          '确认撤销',
          () => {
            set({
              credentials: get().credentials.map((c) =>
                c.id === id ? { ...c, status: '已撤销' } : c,
              ),
            })
            get().toastMsg('凭证已撤销（演示）')
          },
        )
      },

      openUpload: () => {
        const { scope } = useChat.getState()
        const libs = getLibraries()
        const target =
          scope.length === 1 && libs.some((l) => l.id === scope[0] && l.role !== '查看者')
            ? scope[0]
            : (libs.find((l) => l.role !== '查看者')?.id ?? '')
        set({
          pendingUpload: [],
          uploadTarget: target,
          uploadStarted: false,
          modal: { kind: 'upload' },
        })
      },
      addPendingFiles: (files) => {
        if (get().uploadStarted) return
        const pending = [...get().pendingUpload]
        const errors: string[] = []
        for (const file of files) {
          const error = documentUploadError(file)
          if (error) {
            errors.push(error)
            continue
          }
          if (
            !pending.some(
              (item) =>
                item.name === file.name &&
                item.size === file.size &&
                item.lastModified === file.lastModified,
            )
          )
            pending.push(file)
        }
        set({ pendingUpload: pending })
        if (errors.length) get().toastMsg(errors.join('；'), true)
      },
      removePending: (index) =>
        set({ pendingUpload: get().pendingUpload.filter((_, i) => i !== index) }),
      setUploadTarget: (id) => set({ uploadTarget: id }),
      setUploadStarted: (started) => set({ uploadStarted: started }),

      saveModelConfig: (kind, cfg) => {
        set({ modelConfig: { ...get().modelConfig, [kind]: cfg } })
        get().toastMsg('模型演示配置已保存，未连接实际服务')
      },
      setLastConfigClient: (c) => set({ lastConfigClient: c }),

      copyText: async (text) => {
        try {
          await navigator.clipboard.writeText(text)
          get().toastMsg('已复制到剪贴板')
        } catch {
          set({ modal: { kind: 'copy', text } })
        }
      },
      resetDemo: () => {
        get().confirmThen(
          '重置演示',
          '将清除当前浏览器中保存的原型修改，恢复初始示例数据。',
          '确认重置',
          () => {
            try {
              localStorage.removeItem(STORE_KEY)
            } catch {
              /* ignore */
            }
            location.reload()
          },
        )
      },
      confirmThen: (title, text, confirmLabel, fn) =>
        set({ modal: { kind: 'confirm', title, text, confirmLabel }, modalConfirm: fn }),
    }),
    {
      name: STORE_KEY,
      // 旧版本本地文档与会话不再恢复，避免与服务器状态混用。
      merge: (persisted, current) => {
        const saved = persisted as Partial<KbState> | undefined
        return {
          ...current,
          credentials: saved?.credentials ?? current.credentials,
          modelConfig: saved?.modelConfig ?? current.modelConfig,
        }
      },
      partialize: (state) => ({ credentials: state.credentials, modelConfig: state.modelConfig }),
    },
  ),
)

/** MCP 配置示例，仅供尚未对接的 MCP 管理页使用。 */
export function currentConfigText(client: string) {
  return configText(client)
}
