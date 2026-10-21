import { CreateLibraryModal } from '@/features/knowledge/CreateLibraryModal'
import { useLibraries } from '@/features/knowledge/useLibraries'
import { useState, type FormEvent, type ReactNode } from 'react'

import { formatSize } from '@/mock/logic'
import { useKb } from '@/stores/knowledge'

import { Icon } from './Icon'

const ACCEPT = /\.(pdf|docx?|pptx?|xlsx?|txt|md|jpe?g|png|webp|bmp)$/i

/** 弹窗外壳：默认用 store 的关闭动作，功能页的表单弹窗可传自己的 `onClose`。 */
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
    <div className="modal-backdrop">
      <section
        className="modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="modal-title"
        tabIndex={-1}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="modal-head">
          <h2 id="modal-title">{title}</h2>
          <button className="icon-btn" onClick={close} aria-label="关闭对话框">
            <Icon name="close" />
          </button>
        </div>
        {description && <p className="modal-description">{description}</p>}
        {children}
      </section>
    </div>
  )
}

/** 上传入库弹窗。 */
function UploadModal() {
  const { libs } = useLibraries()
  const pending = useKb((s) => s.pendingUpload)
  const target = useKb((s) => s.uploadTarget)
  const started = useKb((s) => s.uploadStarted)
  const setTarget = useKb((s) => s.setUploadTarget)
  const removePending = useKb((s) => s.removePending)
  const addFiles = useKb((s) => s.addPendingFiles)
  const startUpload = useKb((s) => s.startUpload)
  const closeModal = useKb((s) => s.closeModal)
  const toastMsg = useKb((s) => s.toastMsg)
  const [dragging, setDragging] = useState(false)

  const collect = (files: File[]) => {
    const valid = files.filter((f) => ACCEPT.test(f.name))
    if (valid.length !== files.length)
      toastMsg('已跳过不支持的格式。请上传文档、笔记或常见图片。', true)
    addFiles(valid.map((f) => ({ name: f.name, size: f.size })))
  }

  return (
    <ModalShell
      title="留住每一份灵感"
      description="将文档、笔记或图片收进知识库。此处演示上传与解析流程，文件不会发送到服务器。"
    >
      <label className="field">
        保存到
        <select value={target} disabled={started} onChange={(e) => setTarget(e.target.value)}>
          {libs
            .filter((l) => l.role !== '查看者')
            .map((l) => (
              <option key={l.id} value={l.id}>
                {l.name}
              </option>
            ))}
        </select>
      </label>

      <button
        type="button"
        className={`dropzone ${dragging ? 'dragging' : ''}`}
        onClick={() => document.getElementById('upload-input')?.click()}
        onDragOver={(e) => {
          e.preventDefault()
          setDragging(true)
        }}
        onDragLeave={() => setDragging(false)}
        onDrop={(e) => {
          e.preventDefault()
          setDragging(false)
          collect([...e.dataTransfer.files])
        }}
      >
        <Icon name="upload" />
        <strong>点击选择，或把文件轻放在这里</strong>
        <small>
          PDF、Word、PPT、Excel、TXT、Markdown 与图片
          <br />
          支持多文件选择
        </small>
      </button>

      <div>
        {pending.map((f, i) => (
          <div className="upload-row" key={`${f.name}-${f.size}`}>
            <Icon name="file" />
            <span>{f.name}</span>
            <small>{formatSize(f.size)}</small>
            <button
              className="icon-btn"
              onClick={() => removePending(i)}
              aria-label={`移除${f.name}`}
            >
              <Icon name="close" />
            </button>
          </div>
        ))}
      </div>

      {started && (
        <div>
          <div className="upload-progress">
            <div />
          </div>
          <p className="table-note">正在处理示例任务</p>
        </div>
      )}

      <div className="form-actions">
        <button className="btn ghost" onClick={closeModal} disabled={started}>
          稍后再说
        </button>
        <button
          className="btn primary"
          onClick={() => void startUpload()}
          disabled={!pending.length || started}
        >
          开始入库{pending.length ? ` · ${pending.length}` : ''}
        </button>
      </div>
    </ModalShell>
  )
}

/** 新建凭证弹窗。 */
function CredentialModal() {
  const [name, setName] = useState('')
  const [days, setDays] = useState('90')
  const createCredential = useKb((s) => s.createCredential)
  const closeModal = useKb((s) => s.closeModal)
  const toastMsg = useKb((s) => s.toastMsg)

  const submit = (e: FormEvent) => {
    e.preventDefault()
    const value = name.trim()
    if (!value) {
      toastMsg('请填写凭证名称', true)
      return
    }
    createCredential(value, Number(days))
  }

  return (
    <ModalShell
      title="为工具打开一扇门"
      description="新凭证默认只读。访问范围始终跟随你当前有权访问的知识库。"
    >
      <form onSubmit={submit}>
        <label className="field">
          凭证名称
          <input
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
            maxLength={50}
            placeholder="例如：我的 Codex"
            autoFocus
          />
        </label>
        <label className="field">
          有效期
          <select value={days} onChange={(e) => setDays(e.target.value)}>
            <option value="90">90 天 · 默认</option>
            <option value="30">30 天</option>
            <option value="7">7 天</option>
          </select>
        </label>
        <div className="info-banner">
          <Icon name="lock" />
          <span>
            只读工具：search、search_images、ask、list_knowledge_bases。写入工具本期不开放。
          </span>
        </div>
        <div className="form-actions">
          <button type="button" className="btn ghost" onClick={closeModal}>
            取消
          </button>
          <button className="btn primary" type="submit">
            创建演示凭证
          </button>
        </div>
      </form>
    </ModalShell>
  )
}

/** 一次性令牌弹窗。 */
function TokenModal({ token }: { token: string }) {
  const closeModal = useKb((s) => s.closeModal)
  const copyText = useKb((s) => s.copyText)
  return (
    <ModalShell
      title="你的连接已准备好"
      description="这是无真实访问能力的演示令牌。仅展示这一次，关闭后无法重新查看。"
    >
      <div className="info-banner">
        <Icon name="shield" />
        <span>请妥善保管令牌。演示令牌不会保存至浏览器存储。</span>
      </div>
      <pre className="code-box">{token}</pre>
      <div className="form-actions">
        <button className="btn" onClick={() => void copyText(token)}>
          <Icon name="copy" />
          复制演示令牌
        </button>
        <button className="btn primary" onClick={closeModal}>
          已妥善保存
        </button>
      </div>
    </ModalShell>
  )
}

/** 向量模型更新确认弹窗。 */
function VectorConfirmModal() {
  const closeModal = useKb((s) => s.closeModal)
  const confirm = useKb((s) => s.modalConfirm)
  return (
    <ModalShell
      title="更新向量模型"
      description="更换向量模型需要重建索引。此原型仅演示设置流程，不执行真实重建。"
    >
      <div className="form-actions">
        <button className="btn ghost" onClick={closeModal}>
          取消
        </button>
        <button
          className="btn primary"
          onClick={() => {
            closeModal()
            confirm?.()
          }}
        >
          保存演示配置
        </button>
      </div>
    </ModalShell>
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
    case 'credential':
      return <CredentialModal />
    case 'token':
      return <TokenModal token={modal.token} />
    case 'library':
      return <CreateLibraryModal />
    case 'vector-confirm':
      return <VectorConfirmModal />
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
