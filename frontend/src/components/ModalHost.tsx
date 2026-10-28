import { UploadModal } from '@/features/documents/UploadModal'
import { CreateLibraryModal } from '@/features/knowledge/CreateLibraryModal'
import type { ReactNode } from 'react'
import { Modal } from 'antd'

import { useKb } from '@/stores/knowledge'

import { Icon } from './Icon'

/** 统一弹窗外壳，由 Ant Design 处理焦点、键盘与滚动锁定。 */
export function ModalShell({
  title,
  description,
  children,
  onClose,
}: {
  title: string
  description?: string
  children: ReactNode
  onClose?: () => void
}) {
  const closeModal = useKb((s) => s.closeModal)
  const close = onClose ?? closeModal
  return (
    <Modal
      open
      centered
      width={560}
      title={title}
      footer={null}
      onCancel={close}
      mask={{ closable: false }}
      closable={{ closeIcon: <Icon name="close" />, 'aria-label': '关闭对话框' }}
      className="app-modal"
      classNames={{ container: 'app-dialog', mask: 'app-dialog-mask' }}
    >
      {description && <p className="modal-description">{description}</p>}
      {children}
    </Modal>
  )
}

/** 手动复制弹窗（浏览器限制剪贴板时兜底）。 */
function CopyModal({ text }: { text: string }) {
  const closeModal = useKb((s) => s.closeModal)
  return (
    <ModalShell title="复制内容" description="当前浏览器限制自动复制。选中下方内容即可手动复制。">
      <textarea
        readOnly
        className="code-box"
        style={{ width: '100%', minHeight: 130 }}
        value={text}
        onFocus={(e) => e.target.select()}
        autoFocus
      />
      <div className="form-actions">
        <button className="btn primary" onClick={closeModal}>
          完成
        </button>
      </div>
    </ModalShell>
  )
}

/** 破坏性操作二次确认弹窗。 */
function ConfirmModal({
  title,
  text,
  confirmLabel,
}: {
  title: string
  text: string
  confirmLabel: string
}) {
  const closeModal = useKb((s) => s.closeModal)
  const confirm = useKb((s) => s.modalConfirm)
  return (
    <ModalShell title={title} description={text}>
      <div className="form-actions">
        <button className="btn ghost" onClick={closeModal}>
          取消
        </button>
        <button
          className="btn danger"
          onClick={() => {
            closeModal()
            confirm?.()
          }}
        >
          {confirmLabel}
        </button>
      </div>
    </ModalShell>
  )
}

/** 弹窗宿主：按 store 中的 modal 类型渲染对应弹窗。 */
export function ModalHost() {
  const modal = useKb((s) => s.modal)
  if (!modal) return null
  switch (modal.kind) {
    case 'upload':
      return <UploadModal />
    case 'library':
      return <CreateLibraryModal />
    case 'copy':
      return <CopyModal text={modal.text} />
    case 'confirm':
      return (
        <ConfirmModal title={modal.title} text={modal.text} confirmLabel={modal.confirmLabel} />
      )
    default:
      return null
  }
}
