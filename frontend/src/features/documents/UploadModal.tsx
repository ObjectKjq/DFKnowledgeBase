import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useRef, useState } from 'react'

import { uploadDoc } from '@/api/docController'
import { Icon } from '@/components/Icon'
import { ModalShell } from '@/components/ModalHost'
import { useLibraries } from '@/features/knowledge/useLibraries'
import { formatSize } from '@/mock/logic'
import { useKb } from '@/stores/knowledge'

import { DOCUMENT_ACCEPT } from './documents'

/** 多文件上传：发送真实文件，成功项移出队列，失败项保留供再次上传。 */
export function UploadModal() {
  const { libs } = useLibraries()
  const client = useQueryClient()
  const input = useRef<HTMLInputElement>(null)
  const [dragging, setDragging] = useState(false)
  const pending = useKb((s) => s.pendingUpload)
  const target = useKb((s) => s.uploadTarget)
  const started = useKb((s) => s.uploadStarted)
  const setTarget = useKb((s) => s.setUploadTarget)
  const removePending = useKb((s) => s.removePending)
  const addFiles = useKb((s) => s.addPendingFiles)
  const closeModal = useKb((s) => s.closeModal)
  const toastMsg = useKb((s) => s.toastMsg)
  const writable = libs.filter((library) => library.role !== '查看者')
  const canUpload = writable.some((library) => library.id === target)

  const mutation = useMutation({
    mutationFn: ({ files, knowledgeBaseId }: { files: File[]; knowledgeBaseId: number }) =>
      Promise.allSettled(
        files.map((file) => {
          const data = new FormData()
          data.append('file', file)
          // 覆盖生成请求的 JSON 默认值，由浏览器补齐 multipart boundary。
          return uploadDoc({ knowledgeBaseId }, data, { headers: { 'Content-Type': undefined } })
        }),
      ),
    onMutate: () => useKb.getState().setUploadStarted(true),
    onSuccess: (results) => {
      const count = results.filter((result) => result.status === 'fulfilled').length
      for (let index = results.length - 1; index >= 0; index--) {
        if (results[index].status === 'fulfilled') removePending(index)
      }
      if (count) {
        void client.invalidateQueries({ queryKey: ['manage-documents'] })
        toastMsg(`${count} 份文档已上传，入库状态请在文档管理中查看`)
      }
    },
    onSettled: (results) => {
      useKb.getState().setUploadStarted(false)
      if (results?.every((result) => result.status === 'fulfilled')) closeModal()
    },
  })

  return (
    <ModalShell
      title="上传文档"
      description="支持多选与拖拽，单文件上限 50 MB。上传成功后由服务器解析并建立索引。"
    >
      <label className="field">
        保存到
        <select
          aria-label="上传目标知识库"
          value={target}
          disabled={started}
          onChange={(e) => setTarget(e.target.value)}
        >
          {!canUpload && <option value="">请选择可编辑的知识库</option>}
          {writable.map((library) => (
            <option key={library.id} value={library.id}>
              {library.name}
            </option>
          ))}
        </select>
      </label>
      {!writable.length && (
        <p className="field-hint">暂无可上传的知识库，查看者仅可检索和查看文档。</p>
      )}
      <input
        ref={input}
        type="file"
        className="hidden"
        aria-label="选择上传文档"
        multiple
        accept={DOCUMENT_ACCEPT}
        disabled={started}
        onChange={(e) => {
          addFiles([...(e.target.files ?? [])])
          e.target.value = ''
        }}
      />
      <button
        type="button"
        disabled={started}
        className={`dropzone ${dragging ? 'dragging' : ''}`}
        onClick={() => input.current?.click()}
        onDragOver={(e) => {
          e.preventDefault()
          if (!started) setDragging(true)
        }}
        onDragLeave={() => setDragging(false)}
        onDrop={(e) => {
          e.preventDefault()
          setDragging(false)
          if (!started) addFiles([...e.dataTransfer.files])
        }}
      >
        <Icon name="upload" />
        <strong>点击选择，或把文档轻放在这里</strong>
        <small>PDF、Word、PPT、Excel、TXT、Markdown · 单文件 ≤ 50 MB</small>
      </button>
      <div aria-live="polite">
        {pending.map((file, index) => (
          <div className="upload-row" key={`${file.name}-${file.size}-${file.lastModified}`}>
            <Icon name="file" />
            <span>{file.name}</span>
            <small>{formatSize(file.size)}</small>
            <button
              className="icon-btn"
              disabled={started}
              onClick={() => removePending(index)}
              aria-label={`移除${file.name}`}
            >
              <Icon name="close" />
            </button>
          </div>
        ))}
      </div>
      {started && (
        <p className="table-note" role="status">
          正在上传 {pending.length} 份文档，请稍候…
        </p>
      )}
      {!started && mutation.data?.some((result) => result.status === 'rejected') && (
        <p className="field-hint" role="alert">
          部分文档上传失败，失败项已保留，请重试。失败原因见接口提示。
        </p>
      )}
      <div className="form-actions">
        <button className="btn ghost" onClick={closeModal} disabled={started}>
          取消
        </button>
        <button
          className="btn primary"
          disabled={!pending.length || started || !canUpload}
          onClick={() => mutation.mutate({ files: pending, knowledgeBaseId: Number(target) })}
        >
          开始上传{pending.length ? ` · ${pending.length}` : ''}
        </button>
      </div>
    </ModalShell>
  )
}
