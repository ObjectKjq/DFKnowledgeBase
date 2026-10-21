import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'

import {
  getMemberList,
  inviteMember,
  removeMember,
  updateMemberRole,
} from '@/api/knowledgeBaseMemberController'
import { ModalShell } from '@/components/ModalHost'
import { useAuthStore } from '@/stores/auth'
import type { Library } from '@/features/knowledge/libraries'
import { useKb } from '@/stores/knowledge'

/** 共享知识库成员列表与所有者的成员管理操作。 */
export function MembersModal({ library, onClose }: { library: Library; onClose: () => void }) {
  const client = useQueryClient()
  const toastMsg = useKb((s) => s.toastMsg)
  const [username, setUsername] = useState('')
  const [role, setRole] = useState(2)
  const [removing, setRemoving] = useState<number | null>(null)
  const knowledgeBaseId = Number(library.id)
  const canManage = library.role === '所有者'
  const token = useAuthStore((s) => s.token)
  const key = ['knowledge-members', knowledgeBaseId, token]
  const query = useQuery({
    queryKey: key,
    queryFn: async () => (await getMemberList({ knowledgeBaseId })).data ?? [],
  })
  const refresh = () => {
    void client.invalidateQueries({ queryKey: key })
    void client.invalidateQueries({ queryKey: ['knowledge-libraries'] })
  }
  const invite = useMutation({
    mutationFn: inviteMember,
    onSuccess: () => {
      setUsername('')
      toastMsg('成员已邀请')
      refresh()
    },
  })
  const update = useMutation({
    mutationFn: updateMemberRole,
    onSuccess: () => {
      toastMsg('成员角色已更新')
      refresh()
    },
  })
  const remove = useMutation({
    mutationFn: (id: number) => removeMember({ knowledgeBaseId, id }),
    onSuccess: () => {
      setRemoving(null)
      toastMsg('成员已移除')
      refresh()
    },
  })
  const pending = invite.isPending || update.isPending || remove.isPending
  const submit = (event: FormEvent) => {
    event.preventDefault()
    if (canManage && username.trim() && !pending)
      invite.mutate({ knowledgeBaseId, username: username.trim(), memberRole: role })
  }
  return (
    <ModalShell
      title={`${library.name} · 成员`}
      description="所有者管理成员；编辑者可编辑文档，查看者仅可检索和查看。"
      onClose={onClose}
    >
      {canManage && (
        <form onSubmit={submit}>
          <label className="field">
            用户账号
            <input required value={username} onChange={(e) => setUsername(e.target.value)} />
          </label>
          <label className="field">
            成员角色
            <select value={role} onChange={(e) => setRole(Number(e.target.value))}>
              <option value={1}>编辑者</option>
              <option value={2}>查看者</option>
            </select>
          </label>
          <div className="form-actions">
            <button className="btn primary" disabled={pending}>
              邀请成员
            </button>
          </div>
        </form>
      )}
      <p className="table-note">所有者不在成员列表中，其权限不受成员角色影响。</p>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>用户</th>
              <th>成员角色</th>
              {canManage && <th>操作</th>}
            </tr>
          </thead>
          <tbody>
            {(query.data ?? []).map((member) => (
              <tr key={member.id}>
                <td>
                  {member.nickname || member.username}
                  <div className="field-hint">{member.username}</div>
                </td>
                <td>
                  {canManage ? (
                    <select
                      aria-label={`${member.username}的成员角色`}
                      value={member.memberRole}
                      disabled={pending}
                      onChange={(e) =>
                        update.mutate({
                          knowledgeBaseId,
                          id: member.id!,
                          memberRole: Number(e.target.value),
                        })
                      }
                    >
                      <option value={1}>编辑者</option>
                      <option value={2}>查看者</option>
                    </select>
                  ) : member.memberRole === 1 ? (
                    '编辑者'
                  ) : (
                    '查看者'
                  )}
                </td>
                {canManage && (
                  <td>
                    {removing === member.id ? (
                      <span className="table-actions">
                        <button disabled={pending} onClick={() => remove.mutate(member.id!)}>
                          确认移除
                        </button>
                        <button disabled={pending} onClick={() => setRemoving(null)}>
                          取消
                        </button>
                      </span>
                    ) : (
                      <button
                        className="btn ghost"
                        disabled={pending}
                        onClick={() => setRemoving(member.id!)}
                      >
                        移除
                      </button>
                    )}
                  </td>
                )}
              </tr>
            ))}
            {!query.data?.length && (
              <tr>
                <td colSpan={canManage ? 3 : 2}>
                  {query.isPending ? '正在加载成员…' : query.isError ? '成员加载失败' : '暂无成员'}
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
      {query.isError && (
        <button className="btn ghost" onClick={() => void query.refetch()}>
          重试加载
        </button>
      )}
    </ModalShell>
  )
}
