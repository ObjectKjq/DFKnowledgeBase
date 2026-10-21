import { useQuery } from '@tanstack/react-query'
import { useEffect, useRef } from 'react'

import { getPreview } from '@/api/docSearchController'
import { Icon } from '@/components/Icon'
import { useAuthStore } from '@/stores/auth'
import { useChat } from '@/stores/chat'

/** 引用预览每次打开都重新校验权限与文档状态，并定位到命中分块。 */
export function PreviewDrawer() {
  const chunkId = useChat((s) => s.previewChunkId)
  const close = useChat((s) => s.closePreview)
  const token = useAuthStore((s) => s.token)
  const hit = useRef<HTMLQuoteElement>(null)
  const query = useQuery({
    queryKey: ['chat-preview', token, chunkId],
    queryFn: async () => {
      const { data } = await getPreview({ chunkId: chunkId! }, { silent: true })
      if (!data) throw new Error('引用原文已不可用')
      return data
    },
    enabled: chunkId !== null && !!token,
    retry: false,
    gcTime: 0,
  })
  useEffect(() => {
    if (query.data && !query.isFetching)
      hit.current?.scrollIntoView({ block: 'center', behavior: 'smooth' })
  }, [query.data, query.isFetching])
  if (chunkId === null) return <div className="drawer" id="drawer" />
  const chunk = query.data
  return (
    <aside className="drawer open" id="drawer" aria-label="引用原文预览">
      <div className="drawer-head">
        <div>
          <strong>原文预览</strong>
          <p>{!query.isFetching && !query.isError ? chunk?.docTitle : '正在校验引用'}</p>
        </div>
        <button className="icon-btn" onClick={close} aria-label="关闭原文预览">
          <Icon name="close" />
        </button>
      </div>
      <div className="drawer-body">
        {query.isFetching ? (
          <p role="status">正在校验权限并加载原文…</p>
        ) : query.isError ? (
          <div className="empty-result" role="alert">
            <h3>引用已不可用</h3>
            <p>原文可能已删除，或你已无权访问。</p>
            <p>{query.error.message}</p>
            <button className="btn ghost" onClick={() => void query.refetch()}>
              重新加载
            </button>
          </div>
        ) : (
          chunk && (
            <article className="document-page">
              <div className="doc-kicker">{chunk.knowledgeBaseName}</div>
              <h2>{chunk.docTitle}</h2>
              <p className="table-note">
                {chunk.pageNumber ? `第 ${chunk.pageNumber} 页 · ` : ''}第{' '}
                {(chunk.chunkIndex ?? 0) + 1} 个分块 · 字符 {chunk.startOffset ?? 0}–
                {chunk.endOffset ?? 0}
              </p>
              <blockquote ref={hit} id="preview-hit" className="chat-plain-text">
                {chunk.text}
              </blockquote>
            </article>
          )
        )}
      </div>
      <div className="drawer-foot">
        <span>
          {chunk && !query.isFetching && !query.isError
            ? '已定位至命中分块'
            : '引用与文档权限实时校验'}
        </span>
      </div>
    </aside>
  )
}
