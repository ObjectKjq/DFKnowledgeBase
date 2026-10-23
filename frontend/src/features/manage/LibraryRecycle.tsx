import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import {
  getDeletedKnowledgeBases,
  purgeKnowledgeBase,
  restoreKnowledgeBase,
} from '@/api/knowledgeBaseController'
import { useAuthStore } from '@/stores/auth'
import { useKb } from '@/stores/knowledge'

/** 原所有者查看回收站，保留期内恢复成员关系或再次确认彻底删除。 */
export function LibraryRecycle() {
  const token = useAuthStore((s) => s.token)
  const client = useQueryClient()
  const confirm = useKb((s) => s.confirmThen)
  const toast = useKb((s) => s.toastMsg)
  const query = useQuery({
    queryKey: ['knowledge-recycle', token],
    queryFn: async () => ({
      rows: (await getDeletedKnowledgeBases()).data ?? [],
      checkedAt: Date.now(),
    }),
  })
  const refresh = () => {
    void client.invalidateQueries({ queryKey: ['knowledge-recycle'] })
    void client.invalidateQueries({ queryKey: ['knowledge-libraries'] })
    void client.invalidateQueries({ queryKey: ['manage-documents'] })
  }
  const restore = useMutation({
    mutationFn: (id: number) => restoreKnowledgeBase({ id }),
    onSuccess: () => {
      toast('知识库及原成员关系已恢复，文档待索引就绪后可检索')
      refresh()
    },
  })
  const purge = useMutation({
    mutationFn: (id: number) => purgeKnowledgeBase({ id, confirmed: true }),
    onSuccess: () => {
      toast('知识库已彻底删除')
      refresh()
    },
  })
  return (
    <>
      <p className="table-note">
        知识库删除后保留 30 天，仅原所有者可以恢复，并保留原成员关系。彻底删除不可恢复。
      </p>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>知识库名称</th>
              <th>删除时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {query.data?.rows.map((library) => {
              const canRestore =
                !!library.deleteTime &&
                new Date(library.deleteTime).getTime() + 30 * 86400000 > query.data.checkedAt
              return (
                <tr key={library.id}>
                  <td>{library.name}</td>
                  <td>{library.deleteTime?.replace('T', ' ') ?? '—'}</td>
                  <td>
                    <span className="table-actions">
                      <button
                        disabled={!canRestore || restore.isPending || purge.isPending}
                        onClick={() => restore.mutate(library.id!)}
                      >
                        恢复
                      </button>
                      <button
                        disabled={restore.isPending || purge.isPending}
                        onClick={() =>
                          confirm(
                            '彻底删除知识库',
                            `「${library.name}」及文档将被永久删除，此操作不可恢复。`,
                            '确认彻底删除',
                            () => purge.mutate(library.id!),
                          )
                        }
                      >
                        彻底删除
                      </button>
                    </span>
                    {!canRestore && <small>保留期已结束</small>}
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
      {query.isPending && <p role="status">正在加载回收站…</p>}
      {query.isSuccess && !query.data.rows.length && <p>知识库回收站为空</p>}
      {query.isError && <button onClick={() => void query.refetch()}>回收站加载失败，重试</button>}
    </>
  )
}
