import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'

import { createKnowledgeBase } from '@/api/knowledgeBaseController'
import { ModalShell } from '@/components/ModalHost'
import { useKb } from '@/stores/knowledge'

/** 创建个人或共享知识库。 */
export function CreateLibraryModal() {
  const [name, setName] = useState('')
  const [type, setType] = useState(1)
  const [description, setDescription] = useState('')
  const closeModal = useKb((s) => s.closeModal)
  const toastMsg = useKb((s) => s.toastMsg)
  const client = useQueryClient()
  const mutation = useMutation({
    mutationFn: createKnowledgeBase,
    onSuccess: () => {
      void client.invalidateQueries({ queryKey: ['knowledge-libraries'] })
      closeModal()
      toastMsg('知识库已创建')
    },
  })
  const submit = (event: FormEvent) => {
    event.preventDefault()
    if (!name.trim() || mutation.isPending) return
    mutation.mutate({ name: name.trim(), type, description: description.trim() || undefined })
  }
  return (
    <ModalShell title="新建知识库" description="个人知识库仅自己可见；共享知识库可邀请成员。">
      <form onSubmit={submit}>
        <label className="field">
          知识库名称
          <input
            value={name}
            onChange={(e) => setName(e.target.value)}
            maxLength={50}
            required
            autoFocus
          />
        </label>
        <label className="field">
          知识库类型
          <select value={type} onChange={(e) => setType(Number(e.target.value))}>
            <option value={1}>个人知识库</option>
            <option value={2}>共享知识库</option>
          </select>
        </label>
        <label className="field">
          说明 · 选填
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            maxLength={200}
          />
        </label>
        <div className="form-actions">
          <button type="button" className="btn ghost" onClick={closeModal}>
            取消
          </button>
          <button className="btn primary" disabled={mutation.isPending}>
            创建知识库
          </button>
        </div>
      </form>
    </ModalShell>
  )
}
