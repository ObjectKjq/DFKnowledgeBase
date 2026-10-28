import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Select } from 'antd'
import { useRef, useState } from 'react'

import { createDoc } from '@/api/docController'
import { uploadFile } from '@/api/fileController'
import { Icon } from '@/components/Icon'
import { ModalShell } from '@/components/ModalHost'
import { useLibraries } from '@/features/knowledge/useLibraries'
import { formatSize } from '@/lib/format'
import { useKb } from '@/stores/knowledge'

import { DOCUMENT_ACCEPT } from './documents'

/** 选择文件后立即上传，确认后使用文件引用创建文档。 */
export function UploadModal() {
  const { libs } = useLibraries()
  const client = useQueryClient()
  const input = useRef<HTMLInputElement>(null)
  const [dragging, setDragging] = useState(false)
  const [uploaded, setUploaded] = useState(new Map<File, { id: number; url: string }>())
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

  const upload = useMutation({
    mutationFn: (files: File[]) =>
      Promise.allSettled(
        files.map(async (file) => {
          const data = new FormData()
          data.append('file', file)
          // 文件放入 FormData，由浏览器补齐 multipart boundary。
          const result = await uploadFile({ file: undefined }, data, {
            headers: { 'Content-Type': undefined },
          })
          const reference = result.data
          if (!reference?.id || !reference.url) throw new Error('文件上传响应缺少文件引用')
          return { file, id: reference.id, url: reference.url }
        }),
      ),
    onMutate: () => useKb.getState().setUploadStarted(true),
    onSuccess: (results) => {
      setUploaded((current) => {
        const next = new Map(current)
        for (const result of results) {
          if (result.status === 'fulfilled') {
            const { file, id, url } = result.value
            next.set(file, { id, url })
          }
        }
        return next
      })
    },
    onSettled: () => useKb.getState().setUploadStarted(false),
  })
  const selectFiles = (files: File[]) => {
    if (useKb.getState().uploadStarted) return
    addFiles(files)
    const remaining = useKb.getState().pendingUpload.filter((file) => !uploaded.has(file))
    if (remaining.length) upload.mutate(remaining)
  }

  const creation = useMutation({
    mutationFn: ({ files, knowledgeBaseId }: { files: File[]; knowledgeBaseId: number }) =>
      Promise.allSettled(
        files.map((file) => {
          const reference = uploaded.get(file)!
          return createDoc({ knowledgeBaseId, fileId: reference.id, fileUrl: reference.url })
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
        toastMsg(`${count} 份文档已创建，入库状态请在文档管理中查询`)
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
      description="支持多选与拖拽，单文件上限 50 MB。选中文件后立即上传，创建文档后由服务器解析并建立索引。"
    >
      <label className="field">
        保存到
        <Select
          aria-label="上传目标知识库"
          value={canUpload ? target : undefined}
          placeholder="请选择可编辑的知识库"
          disabled={started || !writable.length}
          showSearch={{ optionFilterProp: 'label' }}
          options={writable.map((library) => ({ value: library.id, label: library.name }))}
          onChange={setTarget}
        />
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
          selectFiles([...(e.target.files ?? [])])
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
          if (!started) selectFiles([...e.dataTransfer.files])
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
            <small>
              {uploaded.has(file) ? '已上传' : upload.isPending ? '上传中…' : '上传失败'}
            </small>
            {!started && !uploaded.has(file) && (
              <button className="btn ghost" onClick={() => upload.mutate([file])}>
                重试上传
              </button>
            )}
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
          {creation.isPending ? '正在创建文档，请稍候…' : '正在上传文件，请稍候…'}
        </p>
      )}
      {!started && creation.data?.some((result) => result.status === 'rejected') && (
        <p className="field-hint" role="alert">
          部分文档创建失败，失败项已保留，请重试。失败原因见接口提示。
        </p>
      )}
      <div className="form-actions">
        <button className="btn ghost" onClick={closeModal} disabled={started}>
          取消
        </button>
        <button
          className="btn primary"
          disabled={
            !pending.length || started || !canUpload || pending.some((file) => !uploaded.has(file))
          }
          onClick={() => creation.mutate({ files: pending, knowledgeBaseId: Number(target) })}
        >
          创建文档{pending.length ? ` · ${pending.length}` : ''}
        </button>
      </div>
    </ModalShell>
  )
}
