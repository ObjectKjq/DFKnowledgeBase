import { useMutation, useQuery } from '@tanstack/react-query'
import { useEffect, useRef } from 'react'

import { getDocumentPreview } from '@/api/docSearchController'
import { downloadFile } from '@/api/fileController'
import { Icon } from '@/components/Icon'
import { assertResultSuccess } from '@/lib/http'
import { useAuthStore } from '@/stores/auth'
import { useChat } from '@/stores/chat'

/** 下载仍走授权 API，业务错误不得当成文件展示。 */
async function originalFile(id: number, signal?: AbortSignal): Promise<Blob> {
  const blob = (await downloadFile({ id }, { responseType: 'blob', signal, silent: true })) as Blob
  if (blob.type.includes('json')) {
    const result = JSON.parse(await blob.text()) as { code?: number; msg?: string }
    assertResultSuccess(result, true)
    throw new Error(result.msg || '原文件已不可用')
  }
  return blob
}

/** PDF 原始页面用浏览器对象 URL 预览，抽屉关闭或切换后及时回收。 */
function PdfPreview({ blob, page }: { blob: Blob; page: number }) {
  const frame = useRef<HTMLIFrameElement>(null)
  useEffect(() => {
    const value = URL.createObjectURL(blob)
    if (frame.current) frame.current.src = `${value}#page=${page}`
    return () => URL.revokeObjectURL(value)
  }, [blob, page])
  return <iframe ref={frame} className="pdf-preview" title={`PDF 原始页面 · 第 ${page} 页`} />
}

/** 每次打开重新检查内容授权，展示原始 PDF、文本全文或完整 Office 解析内容。 */
export function PreviewDrawer() {
  const chunkId = useChat((s) => s.previewChunkId)
  const close = useChat((s) => s.closePreview)
  const token = useAuthStore((s) => s.token)
  const hit = useRef<HTMLElement>(null)
  const query = useQuery({
    queryKey: ['chat-preview', token, chunkId],
    queryFn: async ({ signal }) => {
      const { data } = await getDocumentPreview({ chunkId: chunkId! }, { signal, silent: true })
      if (!data?.source) throw new Error('引用原文已不可用')
      const source = data.source
      const raw =
        source.fileId && (source.previewType === 'PDF' || source.previewType === 'TEXT')
          ? await originalFile(source.fileId, signal)
          : undefined
      return {
        document: data,
        raw,
        text: source.previewType === 'TEXT' && raw ? await raw.text() : undefined,
      }
    },
    enabled: chunkId !== null && !!token,
    retry: false,
    gcTime: 0,
    staleTime: 0,
  })
  const action = useMutation({
    mutationFn: async (kind: 'open' | 'download') => {
      const source = query.data?.document.source
      if (!source?.fileId) throw new Error('原文件已不可用')
      const view = kind === 'open' ? window.open('about:blank', '_blank') : null
      if (kind === 'open' && !view) throw new Error('浏览器阻止了新页面，请允许弹窗或下载原文件')
      try {
        const blob = await originalFile(source.fileId)
        const url = URL.createObjectURL(blob)
        if (view) {
          view.opener = null
          view.location.href = `${url}${source.previewType === 'PDF' ? `#page=${source.pageNumber ?? 1}` : ''}`
        } else {
          const anchor = document.createElement('a')
          anchor.href = url
          anchor.download = source.docTitle ?? '原文件'
          anchor.click()
        }
        setTimeout(() => URL.revokeObjectURL(url), 60_000)
      } catch (error) {
        view?.close()
        throw error
      }
    },
  })
  useEffect(() => {
    if (query.data && !query.isFetching)
      hit.current?.scrollIntoView({ block: 'center', behavior: 'smooth' })
  }, [query.data, query.isFetching])
  if (chunkId === null) return <div className="drawer" id="drawer" />
  const source = query.data?.document.source
  const text = query.data?.text
  const start = Math.max(0, Math.min(text?.length ?? 0, source?.startOffset ?? 0))
  const end = Math.max(start, Math.min(text?.length ?? 0, source?.endOffset ?? start))
  return (
    <aside className="drawer open" id="drawer" aria-label="引用原文预览">
      <div className="drawer-head">
        <div>
          <strong>原文预览</strong>
          <p>{!query.isFetching && !query.isError ? source?.docTitle : '正在校验引用'}</p>
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
          source && (
            <article className="document-page">
              <div className="doc-kicker">{source.knowledgeBaseName}</div>
              <h2>{source.docTitle}</h2>
              <p className="table-note">
                {source.pageNumber
                  ? source.format === 3
                    ? `第 ${source.pageNumber} 张幻灯片 · `
                    : `第 ${source.pageNumber} 页 · `
                  : ''}
                {source.sheetName && `工作表 ${source.sheetName} · `}字符 {source.startOffset ?? 0}–
                {source.endOffset ?? 0}
                {source.ocr && ' · OCR 文本'}
              </p>
              {source.previewType === 'PDF' && query.data?.raw ? (
                <PdfPreview key={chunkId} blob={query.data.raw} page={source.pageNumber ?? 1} />
              ) : text !== undefined ? (
                <div className="chat-plain-text">
                  {text.slice(0, start)}
                  <mark ref={hit}>{text.slice(start, end)}</mark>
                  {text.slice(end)}
                </div>
              ) : (
                <>
                  <p className="table-note">Office 解析内容，保留页码、幻灯片或工作表信息。</p>
                  {query.data?.document.chunks?.map((chunk) => (
                    <section key={chunk.chunkId}>
                      <p className="table-note">
                        {chunk.pageNumber
                          ? chunk.format === 3
                            ? `第 ${chunk.pageNumber} 张幻灯片`
                            : `第 ${chunk.pageNumber} 页`
                          : ''}
                        {chunk.sheetName && ` · 工作表 ${chunk.sheetName}`}
                      </p>
                      <blockquote
                        className={`chat-plain-text ${chunk.chunkId === chunkId ? 'preview-hit' : ''}`}
                      >
                        <span ref={chunk.chunkId === chunkId ? hit : undefined}>{chunk.text}</span>
                      </blockquote>
                    </section>
                  ))}
                </>
              )}
            </article>
          )
        )}
      </div>
      <div className="drawer-foot">
        <span>
          {source && !query.isFetching && !query.isError
            ? '已定位至命中原文'
            : '引用与文档权限实时校验'}
        </span>
        {source && !query.isFetching && !query.isError && (
          <div className="table-actions">
            <button disabled={action.isPending} onClick={() => action.mutate('open')}>
              新页打开原文件
            </button>
            <button disabled={action.isPending} onClick={() => action.mutate('download')}>
              下载原文件
            </button>
          </div>
        )}
        {action.isError && <p role="alert">{action.error.message}</p>}
      </div>
    </aside>
  )
}
