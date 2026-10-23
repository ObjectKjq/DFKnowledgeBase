/** 共用弹层、提示及临时上传队列；服务端业务数据不写入浏览器存储。 */
import { create } from 'zustand'
import { documentUploadError } from '@/features/documents/documents'
import { getLibraries } from '@/features/knowledge/libraries'
import { useChat } from '@/stores/chat'

export type ModalState =
  | { kind: 'upload' }
  | { kind: 'library' }
  | { kind: 'copy'; text: string }
  | { kind: 'confirm'; title: string; text: string; confirmLabel: string }

interface KbState {
  popover: string
  modal: ModalState | null
  modalConfirm: (() => void) | null
  toast: { msg: string; error: boolean } | null
  pendingUpload: File[]
  uploadTarget: string
  uploadStarted: boolean
  toastMsg: (msg: string, error?: boolean) => void
  clearToast: () => void
  openPopover: (name: string) => void
  closePopover: () => void
  openModal: (modal: ModalState, confirm?: () => void) => void
  closeModal: () => void
  openUpload: () => void
  addPendingFiles: (files: File[]) => void
  removePending: (index: number) => void
  setUploadTarget: (id: string) => void
  setUploadStarted: (started: boolean) => void
  copyText: (text: string) => Promise<void>
  confirmThen: (title: string, text: string, confirmLabel: string, fn: () => void) => void
}

let toastTimer: ReturnType<typeof setTimeout> | undefined
export const useKb = create<KbState>()((set, get) => ({
  popover: '',
  modal: null,
  modalConfirm: null,
  toast: null,
  pendingUpload: [],
  uploadTarget: '',
  uploadStarted: false,
  toastMsg: (msg, error = false) => {
    set({ toast: { msg, error } })
    clearTimeout(toastTimer)
    toastTimer = setTimeout(() => set({ toast: null }), 4200)
  },
  clearToast: () => set({ toast: null }),
  openPopover: (name) => set({ popover: get().popover === name ? '' : name }),
  closePopover: () => set({ popover: '' }),
  openModal: (modal, confirm) => set({ modal, modalConfirm: confirm ?? null }),
  closeModal: () => {
    if (get().uploadStarted) {
      get().toastMsg('正在上传文档，请稍候')
      return
    }
    set({ modal: null, modalConfirm: null, pendingUpload: [] })
  },
  openUpload: () => {
    const { scope } = useChat.getState()
    const libs = getLibraries()
    const target =
      scope.length === 1 &&
      libs.some((library) => library.id === scope[0] && library.role !== '查看者')
        ? scope[0]
        : (libs.find((library) => library.role !== '查看者')?.id ?? '')
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
  setUploadStarted: (uploadStarted) => set({ uploadStarted }),
  copyText: async (text) => {
    try {
      await navigator.clipboard.writeText(text)
      get().toastMsg('已复制到剪贴板')
    } catch {
      set({ modal: { kind: 'copy', text } })
    }
  },
  confirmThen: (title, text, confirmLabel, fn) =>
    set({ modal: { kind: 'confirm', title, text, confirmLabel }, modalConfirm: fn }),
}))
