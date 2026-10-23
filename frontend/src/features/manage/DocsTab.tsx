import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'

import { deleteDoc, getDocPage, purgeDoc, restoreDoc, retryDoc } from '@/api/docController'
import { Icon } from '@/components/Icon'
import { DOCUMENT_FORMATS, DOCUMENT_STATUSES } from '@/features/documents/documents'
import { useLibraries } from '@/features/knowledge/useLibraries'
import { formatSize } from '@/mock/logic'
import { useAuthStore } from '@/stores/auth'
import { useKb } from '@/stores/knowledge'

const PAGE_SIZE = 10

/** 文档管理：服务端分页筛选、状态轮询、失败重试与回收站恢复。 */
export function DocsTab() {
  const { libs } = useLibraries()
  const token = useAuthStore((s) => s.token)
  const userId = useAuthStore((s) => s.user?.id)
  const openUpload = useKb((s) => s.openUpload)
  const confirmThen = useKb((s) => s.confirmThen)
  const toastMsg = useKb((s) => s.toastMsg)
  const client = useQueryClient()
  const [keyword, setKeyword] = useState('')
  const [filters, setFilters] = useState<API.DocPageReqVO>({ pageNum: 1, pageSize: PAGE_SIZE })
  const query = useQuery({
    queryKey: ['manage-documents', userId, token, filters],
    queryFn: async () => {
      const { data } = await getDocPage({ reqVO: filters })
      return { ...(data ?? { list: [], total: 0 }), checkedAt: Date.now() }
    },
    enabled: !!token,
    // 即使筛选「可用」或「失败」，也能发现从其他状态迁入的文档；请求失败后由用户重试。
    refetchInterval: (query) => (query.state.status === 'error' ? false : 3000),
  })
  const refresh = () => client.invalidateQueries({ queryKey: ['manage-documents'] })
  const deletion = useMutation({
    mutationFn: (id: number) => deleteDoc({ id }),
    onSuccess: () => {
      toastMsg('文档已移至回收站，30 天内可恢复')
      void refresh()
    },
  })
  const restoration = useMutation({
    mutationFn: (id: number) => restoreDoc({ id }),
    onSuccess: () => {
      toastMsg('文档已恢复，入库状态以服务器处理结果为准')
      void refresh()
    },
  })
  const retry = useMutation({
    mutationFn: (id: number) => retryDoc({ id }),
    onSuccess: () => {
      toastMsg('已提交重试')
      void refresh()
    },
  })
  const purge = useMutation({
    mutationFn: (id: number) => purgeDoc({ id, confirmed: true }),
    onSuccess: () => {
      toastMsg('文档已彻底删除')
      void refresh()
    },
  })
  const pending = deletion.isPending || restoration.isPending || retry.isPending || purge.isPending
  const rows = query.isError ? [] : (query.data?.list ?? [])
  const total = query.data?.total ?? 0
  const page = filters.pageNum ?? 1
  const pages = Math.max(1, Math.ceil(total / PAGE_SIZE))
  const search = (event: FormEvent) => {
    event.preventDefault()
    setFilters((current) => ({ ...current, pageNum: 1, title: keyword.trim() || undefined }))
  }
  const remove = (doc: API.DocRespVO) =>
    confirmThen(
      '删除文档',
      `「${doc.title || doc.fileName}」将移至回收站，删除后保留 30 天，期内可恢复。`,
      '确认删除',
      () => deletion.mutate(doc.id!),
    )
  const changeFilter = (values: Partial<API.DocPageReqVO>) =>
    setFilters((current) => ({ ...current, ...values, pageNum: 1 }))

  return (
    <>
      <div className="manage-toolbar">
        <form className="manage-toolbar-left" onSubmit={search}>
          <label className="field-search">
            <Icon name="search" />
            <input
              aria-label="搜索文档"
              placeholder="搜索文档标题"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
            />
          </label>
          <select
            className="select"
            aria-label="所属知识库"
            value={filters.knowledgeBaseId ?? ''}
            onChange={(e) =>
              changeFilter({ knowledgeBaseId: e.target.value ? Number(e.target.value) : undefined })
            }
          >
            <option value="">全部有权知识库</option>
            {libs.map((library) => (
              <option key={library.id} value={library.id}>
                {library.name}
              </option>
            ))}
          </select>
          <select
            className="select"
            aria-label="文档状态"
            value={filters.status ?? ''}
            onChange={(e) =>
              changeFilter({ status: e.target.value ? Number(e.target.value) : undefined })
            }
          >
            <option value="">全部文档（含回收站）</option>
            {Object.entries(DOCUMENT_STATUSES).map(([status, label]) => (
              <option key={status} value={status}>
                {status === '6' ? '回收站' : label}
              </option>
            ))}
          </select>
          <button className="btn ghost">查询</button>
        </form>
        <button
          className="btn primary"
          onClick={openUpload}
          disabled={!libs.some((library) => library.role !== '查看者')}
        >
          <Icon name="upload" />
          上传文档
        </button>
      </div>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>文档名称</th>
              <th>格式</th>
              <th>所属知识库</th>
              <th>状态</th>
              <th>上传时间</th>
              <th>大小</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((doc) => {
              const library = libs.find((library) => library.id === String(doc.knowledgeBaseId))
              const canWrite = !!library && library.role !== '查看者'
              const deadline = doc.deleteTime ? new Date(doc.deleteTime) : null
              if (deadline) deadline.setDate(deadline.getDate() + 30)
              const canRestore = !!deadline && deadline.getTime() >= (query.data?.checkedAt ?? 0)
              return (
                <tr key={doc.id}>
                  <td>
                    <span className="table-title">
                      <Icon name="file" />
                      {doc.title || doc.fileName}
                    </span>
                    {doc.status === 5 && (
                      <div className="field-hint doc-failure">
                        {doc.failReason || '入库失败，服务器未返回具体原因'}
                      </div>
                    )}
                    {doc.status === 6 && (
                      <div className="field-hint">
                        删除于 {doc.deleteTime?.replace('T', ' ') ?? '未知时间'} ·{' '}
                        {canRestore ? '30 天内可恢复' : '保留期已结束，无法恢复'}
                      </div>
                    )}
                  </td>
                  <td>{DOCUMENT_FORMATS[doc.format ?? 0] ?? '—'}</td>
                  <td>{library?.name ?? '—'}</td>
                  <td>
                    <span
                      className={`tag ${doc.status === 5 ? 'red' : doc.status === 6 ? 'oat' : ''}`}
                    >
                      {DOCUMENT_STATUSES[doc.status ?? 0] ?? '未知状态'}
                    </span>
                  </td>
                  <td>{doc.createTime?.replace('T', ' ') ?? '—'}</td>
                  <td>{doc.fileSize === undefined ? '—' : formatSize(doc.fileSize)}</td>
                  <td>
                    <span className="table-actions">
                      {canWrite ? (
                        doc.status === 6 ? (
                          <>
                            <button
                              disabled={pending || !canRestore}
                              title="删除后仅 30 天内可恢复"
                              onClick={() => restoration.mutate(doc.id!)}
                            >
                              恢复
                            </button>
                            <button
                              disabled={pending}
                              onClick={() =>
                                confirmThen(
                                  '彻底删除文档',
                                  '文档将被永久删除，此操作不可恢复。',
                                  '确认彻底删除',
                                  () => purge.mutate(doc.id!),
                                )
                              }
                            >
                              彻底删除
                            </button>
                          </>
                        ) : (
                          <>
                            {doc.status === 5 && (
                              <button disabled={pending} onClick={() => retry.mutate(doc.id!)}>
                                重试
                              </button>
                            )}
                            <button disabled={pending} onClick={() => remove(doc)}>
                              删除
                            </button>
                          </>
                        )
                      ) : (
                        <span className="doc-status">仅查看</span>
                      )}
                    </span>
                  </td>
                </tr>
              )
            })}
            {!rows.length && (
              <tr>
                <td colSpan={7} className="empty-cell">
                  {query.isPending
                    ? '正在加载文档…'
                    : query.isError
                      ? '文档加载失败，请重试'
                      : '这里还没有符合条件的文档'}
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
      <div className="pager">
        <span>共 {total} 份文档</span>
        <button
          disabled={page <= 1 || query.isFetching}
          onClick={() => setFilters((current) => ({ ...current, pageNum: page - 1 }))}
        >
          上一页
        </button>
        <span>
          {page} / {pages}
        </span>
        <button
          disabled={page >= pages || query.isFetching}
          onClick={() => setFilters((current) => ({ ...current, pageNum: page + 1 }))}
        >
          下一页
        </button>
      </div>
      <p className="table-note">
        文档状态每 3 秒刷新。删除后在回收站保留 30 天，超期不可恢复。查看者仅可检索和查看文档。
      </p>
    </>
  )
}
