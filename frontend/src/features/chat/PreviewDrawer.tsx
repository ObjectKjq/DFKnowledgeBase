import { useLibraries } from '@/features/knowledge/useLibraries'
import { useEffect } from 'react'

import { IMAGE_ART } from '@/mock/data'
import { libName } from '@/mock/logic'
import { useKb } from '@/stores/knowledge'

import { Icon } from '@/components/Icon'

/** 原文预览抽屉（复刻原型 `.drawer` / renderPreview）。 */
export function PreviewDrawer() {
  const previewId = useKb((s) => s.previewId)
  const docs = useKb((s) => s.docs)
  const { libs } = useLibraries()
  const previewPage = useKb((s) => s.previewPage)
  const contextExpanded = useKb((s) => s.contextExpanded)
  const closePreview = useKb((s) => s.closePreview)
  const previewPageTo = useKb((s) => s.previewPageTo)
  const toggleContext = useKb((s) => s.toggleContext)
  const previewNew = useKb((s) => s.previewNew)

  const doc = previewId ? docs.find((d) => d.id === previewId) : undefined
  const match = !!doc && previewPage === doc.page

  useEffect(() => {
    if (match) {
      requestAnimationFrame(() =>
        document
          .getElementById('preview-hit')
          ?.scrollIntoView({ block: 'center', behavior: 'smooth' }),
      )
    }
  }, [match, previewId, previewPage])

  if (!doc) return <div className="drawer" id="drawer" />

  return (
    <div className="drawer open" id="drawer">
      <div className="drawer-head">
        <div>
          <strong>原文预览</strong>
          <p>
            {doc.title}.{doc.ext}
          </p>
        </div>
        <button className="icon-btn" onClick={closePreview} aria-label="关闭原文预览">
          <Icon name="close" />
        </button>
      </div>
      <div className="drawer-tools">
        <span>{libName(libs, doc.lib)}</span>
        <span>
          <button
            onClick={() => previewPageTo(previewPage - 1)}
            disabled={previewPage <= 1}
            aria-label="上一页"
          >
            ‹
          </button>{' '}
          {previewPage} / {doc.pages}{' '}
          <button
            onClick={() => previewPageTo(previewPage + 1)}
            disabled={previewPage >= doc.pages}
            aria-label="下一页"
          >
            ›
          </button>
        </span>
        <button onClick={previewNew}>
          新页打开
          <Icon name="external" />
        </button>
      </div>
      <div className="drawer-body">
        <article className="document-page">
          <div className="doc-kicker">ZHIXU / LOCAL KNOWLEDGE</div>
          <h2>{doc.title}</h2>
          <p className="table-note">原文样式预览 · 预置示例片段</p>
          {doc.ext === 'png' && (
            <div
              dangerouslySetInnerHTML={{
                __html: IMAGE_ART.replace('viewBox=', 'class="doc-art" viewBox='),
              }}
            />
          )}
          <h3>{doc.section}</h3>
          {match ? (
            <>
              <p>{doc.context || '以下展示导入文件的基本信息。'}</p>
              <blockquote id="preview-hit">
                {doc.text ||
                  '该文件已完成模拟入库。原型未读取其实际文本，不参与有依据的 RAG 生成。'}
              </blockquote>
              <p>{doc.after || `文件名：${doc.title}.${doc.ext}`}</p>
            </>
          ) : (
            <div className="empty-result">
              <p>当前页没有命中片段。</p>
              <button className="btn small" onClick={() => previewPageTo(doc.page)}>
                回到命中位置 · 第 {doc.page} 页
              </button>
            </div>
          )}
          {contextExpanded && (
            <div className="doc-context">
              <h3>前后文 · 相邻分块</h3>
              <p>{doc.context || '该文档没有额外的演示上下文。'}</p>
              <p>{doc.after || ''}</p>
              <p className="table-note">相邻分块仅为当前原型的预置文本。</p>
            </div>
          )}
          <div className="doc-page-number">— {previewPage} —</div>
        </article>
      </div>
      <div className="drawer-foot">
        <span>{match ? '已定位至命中片段' : '翻页预览'}</span>
        <button onClick={toggleContext}>
          <Icon name="layers" />
          {contextExpanded ? '收起上下文' : '展开原文上下文'}
        </button>
      </div>
    </div>
  )
}
