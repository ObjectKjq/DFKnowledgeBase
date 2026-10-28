import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'

import { deleteKnowledgeBase } from '@/api/knowledgeBaseController'
import { Icon } from '@/components/Icon'
import type { Library } from '@/features/knowledge/libraries'
import { useLibraries } from '@/features/knowledge/useLibraries'
import { useKb } from '@/stores/knowledge'

import { MembersModal } from './MembersModal'

/** 知识库与成员管理：所有者管理知识库，成员按角色使用。 */
export function LibrariesTab() {
  const query = useLibraries()
  const client = useQueryClient()
  const openModal = useKb((s) => s.openModal)
  const confirmThen = useKb((s) => s.confirmThen)
  const toastMsg = useKb((s) => s.toastMsg)
  const [members, setMembers] = useState<Library | null>(null)
  const mutation = useMutation({
    mutationFn: (id: string) => deleteKnowledgeBase({ id: Number(id) }),
    onSuccess: () => {
      toastMsg('知识库已删除')
      void client.invalidateQueries({ queryKey: ['knowledge-libraries'] })
      void client.invalidateQueries({ queryKey: ['manage-documents'] })
    },
  })
  const remove = (library: Library) => {
    confirmThen(
      '删除知识库',
      `确认删除「${library.name}」及其文档？删除后不可恢复。`,
      '确认删除',
      () => mutation.mutate(library.id),
    )
  }
  const selected = query.libs.find((library) => library.id === members?.id)
  return (
    <>
      <div className="manage-toolbar">
        <p>管理个人知识库与共享知识库，分配成员角色。</p>
        <button className="btn primary" onClick={() => openModal({ kind: 'library' })}>
          <Icon name="plus" />
          新建知识库
        </button>
      </div>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>知识库名称</th>
              <th>类型</th>
              <th>我的权限</th>
              <th>说明</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {query.libs.map((library) => (
              <tr key={library.id}>
                <td>
                  {library.name}
                  {library.defaultFlag && <span className="tag">默认</span>}
                </td>
                <td>{library.type === 'personal' ? '个人知识库' : '共享知识库'}</td>
                <td>{library.role}</td>
                <td>{library.description || '—'}</td>
                <td>
                  <span className="table-actions">
                    {library.type === 'shared' && (
                      <button onClick={() => setMembers(library)}>成员</button>
                    )}
                    {library.defaultFlag ? (
                      <span className="doc-status">默认知识库不可删除</span>
                    ) : library.role === '所有者' ? (
                      <button disabled={mutation.isPending} onClick={() => remove(library)}>
                        删除
                      </button>
                    ) : (
                      <span className="doc-status">
                        {library.role === '查看者' ? '仅查看' : '可编辑文档'} · 仅所有者可管理
                      </span>
                    )}
                  </span>
                </td>
              </tr>
            ))}
            {!query.libs.length && (
              <tr>
                <td colSpan={5}>
                  {query.isPending
                    ? '正在加载知识库…'
                    : query.isError
                      ? '知识库加载失败'
                      : '暂无知识库'}
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
      <p className="table-note">
        个人知识库不支持成员。共享知识库仅所有者可邀请、修改角色或移除成员；查看者仅可检索和查看文档。
      </p>
      {members && selected && <MembersModal library={selected} onClose={() => setMembers(null)} />}
    </>
  )
}
