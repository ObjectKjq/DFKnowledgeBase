import type { Doc, Message } from '@/mock/data'
import { IMAGE_ART } from '@/mock/data'
import { isTrustQuery, libName } from '@/mock/logic'
import { useKb } from '@/stores/knowledge'

import { Highlight } from '@/components/Highlight'
import { Icon } from '@/components/Icon'

function BrandMini() {
  return (
    <div className="answer-header">
      <span className="mini-mark">
        <Icon name="book" />
      </span>
      知序
      <span className="hint-chip">示例响应</span>
      <small>依据本地示例资料</small>
    </div>
  )
}

function NetworkBlock() {
  return (
    <div className="network-block">
      <span className="tag blue">来自网络</span>
      <p>
        当前为离线交互原型，尚未接入网络搜索服务。联网后，补充结果将在这里独立展示，不计入 RAG
        引用来源。
      </p>
    </div>
  )
}

function Cite({ id, n }: { id: string; n: number }) {
  const showPreview = useKb((s) => s.showPreview)
  return (
    <button className="inline-cite" onClick={() => showPreview(id)} aria-label={`查看引用${n}`}>
      {n}
    </button>
  )
}

/** 带引用答案正文（复刻原型 buildAnswer）。 */
function AnswerBlock({ query, ids }: { query: string; ids: string[] }) {
  const docs = useKb((s) => s.docs)
  const found = ids.map((id) => docs.find((d) => d.id === id)).filter(Boolean) as Doc[]

  if (isTrustQuery(query) && found.some((d) => d.id === 'd1')) {
    const a = found.findIndex((d) => d.id === 'd1')
    const b = found.findIndex((d) => d.id === 'd5')
    return (
      <>
        <p>让知识检索更可信，关键是让“找到什么”和“依据在哪里”始终清晰。结合已有资料，可以从三个方面着手：</p>
        <h3>01 · 兼顾准确匹配与语义理解</h3>
        <p>
          融合关键字与语义检索：用关键字找到编号和术语，用语义找回不同措辞下的相近内容。
          <Cite id={found[a].id} n={a + 1} />
          {b >= 0 && <Cite id={found[b].id} n={b + 1} />}
        </p>
        <h3>02 · 让每条结论都能回到原文</h3>
        <p>
          在答案中附上页级或分块级引用。点击引用，即可在侧边预览中核对原始内容，保留完整的阅读上下文。
          <Cite id={found[a].id} n={a + 1} />
        </p>
        <h3>03 · 依据不足时，明确告诉用户</h3>
        <p>
          没有足够的支撑资料时，说明证据不足，并建议扩大范围或补充文档，避免生成没有出处的答案。
          <Cite id={found[a].id} n={a + 1} />
        </p>
      </>
    )
  }

  return (
    <>
      <p>根据当前范围内的示例资料，与“{query}”相关的内容如下：</p>
      {found.map((d, i) => (
        <div key={d.id}>
          <h3>
            {String(i + 1).padStart(2, '0')} · {d.title}
          </h3>
          <p>
            {d.text || '该上传项尚未接入真实内容解析，无法生成有依据的回答。'}
            <Cite id={d.id} n={i + 1} />
          </p>
        </div>
      ))}
      <p className="table-note">以上为基于预置片段的演示汇总，未调用真实语言模型。</p>
    </>
  )
}

function ResultCard({ doc, query, mode, i }: { doc: Doc; query: string; mode: string; i: number }) {
  const libs = useKb((s) => s.libs)
  const showPreview = useKb((s) => s.showPreview)
  const type =
    doc.ext === 'png' ? '图片' : mode === '精确检索' ? '精确' : mode === '模糊检索' ? '模糊' : doc.id === 'd8' ? 'OCR' : i === 0 ? '精确' : '模糊'
  return (
    <button className="result-card" onClick={() => showPreview(doc.id)}>
      <span className="result-top">
        <h3>
          <Icon name={doc.ext === 'png' ? 'image' : 'file'} />
          {doc.title}
        </h3>
        <span className={`tag ${type === '图片' ? 'blue' : ''}`}>{type}命中</span>
      </span>
      <span className={doc.ext === 'png' ? 'result-thumb' : ''}>
        {doc.ext === 'png' && (
          <span className="thumb" dangerouslySetInnerHTML={{ __html: IMAGE_ART }} />
        )}
        <span className="result-snippet">
          <Highlight text={doc.text || '已导入的文档，内容解析仅作状态演示。'} q={query} />
        </span>
      </span>
      <span className="result-path">
        <Icon name="book" />
        {libName(libs, doc.lib)} › {doc.title} › 第 {doc.page} {doc.ext === 'md' ? '块' : '页'}
        <span className="result-relevance">示例匹配</span>
      </span>
    </button>
  )
}

function CitationCard({ doc, i }: { doc: Doc; i: number }) {
  const libs = useKb((s) => s.libs)
  const showPreview = useKb((s) => s.showPreview)
  return (
    <button className="citation-card" onClick={() => showPreview(doc.id)}>
      <b>{i + 1}</b>
      <div>
        <strong>{doc.title}</strong>
        <small>
          {libName(libs, doc.lib)} · 第 {doc.page} {doc.ext === 'md' ? '块' : '页'}
        </small>
      </div>
    </button>
  )
}

function EmptyResult({
  query,
  rag,
  text,
}: {
  query?: string
  rag?: boolean
  text?: string
}) {
  const broaden = useKb((s) => s.broaden)
  const openUpload = useKb((s) => s.openUpload)
  return (
    <div className="empty-result">
      <Icon name="search" />
      <h3>{rag ? '未找到足够的依据' : '没有找到相关资料'}</h3>
      <p>{text}</p>
      <div className="guide-options" style={{ justifyContent: 'center' }}>
        <button onClick={() => query && void broaden(query)}>扩大范围并试试模糊检索</button>
        <button onClick={openUpload}>补充资料</button>
      </div>
    </div>
  )
}

function GuideMessage({ m }: { m: Message }) {
  const sendQuery = useKb((s) => s.sendQuery)
  const skipGuide = useKb((s) => s.skipGuide)
  return (
    <>
      <div className="answer-content">
        <p>{m.text}</p>
      </div>
      {m.done ? (
        <p className="table-note">已完成本轮引导</p>
      ) : (
        <>
          <div className="guide-options">
            {m.options?.map((o) => (
              <button key={o} onClick={() => void sendQuery(o)}>
                {o}
              </button>
            ))}
          </div>
          <button className="guide-skip" onClick={() => void skipGuide()}>
            跳过引导，直接搜 <Icon name="arrow" />
          </button>
        </>
      )}
    </>
  )
}

function MessageView({ m }: { m: Message }) {
  const docs = useKb((s) => s.docs)
  const toggleFeedback = useKb((s) => s.toggleFeedback)
  const copyText = useKb((s) => s.copyText)

  if (m.type === 'user') {
    return (
      <div className="message message-user">
        <div className="user-bubble">
          {m.image && <img src={m.image} alt="用于以图搜图的查询图片" />}
          {m.text?.split('\n').map((line, i) => (
            <span key={i}>
              {i > 0 && <br />}
              {line}
            </span>
          ))}
          <small>
            {m.scope} · {m.mode}
          </small>
        </div>
      </div>
    )
  }

  const found = (m.ids ?? []).map((id) => docs.find((d) => d.id === id)).filter(Boolean) as Doc[]

  let body = null
  if (m.type === 'results') {
    body = (
      <>
        <p className="result-summary">
          在{m.scope}中找到 <strong>{found.length} 份相关资料</strong>
          <br />
          <small>{m.mode} · 以下为预置资料的交互演示</small>
        </p>
        {found.map((d, i) =>
          d.status === '已删除' ? (
            <div className="empty-result" key={d.id}>
              <p>原文已删除，历史记录保留</p>
            </div>
          ) : (
            <ResultCard key={d.id} doc={d} query={m.query ?? ''} mode={m.mode ?? ''} i={i} />
          ),
        )}
        {m.web && <NetworkBlock />}
      </>
    )
  } else if (m.type === 'answer') {
    body = (
      <>
        <div className="answer-content" id={`answer-${m.id}`}>
          <AnswerBlock query={m.query ?? ''} ids={m.ids ?? []} />
        </div>
        <p className="sources-title">引用来源 · 点击核对原文</p>
        <div className="citation-grid">
          {found.map((d, i) => (
            <CitationCard key={d.id} doc={d} i={i} />
          ))}
        </div>
        {m.web && <NetworkBlock />}
        <div className="response-meta">
          示例模型响应 · {m.model || 'Qwen3'}
          <span style={{ marginLeft: 'auto' }} />
          <button
            onClick={() => copyText(document.getElementById(`answer-${m.id}`)?.textContent ?? '')}
            aria-label="复制回答"
          >
            <Icon name="copy" />
          </button>
          <button
            className={m.feedback ? 'selected' : ''}
            onClick={() => toggleFeedback(m.id)}
            aria-label="这份回答有帮助"
          >
            <Icon name="like" />
          </button>
        </div>
      </>
    )
  } else if (m.type === 'guide') {
    body = <GuideMessage m={m} />
  } else if (m.type === 'empty') {
    body = <EmptyResult query={m.query} rag={m.rag} text={m.text} />
  } else if (m.type === 'note') {
    body = (
      <div className="info-banner">
        <Icon name="shield" />
        <span>{m.text}</span>
      </div>
    )
  }

  return (
    <div className="message">
      <BrandMini />
      {body}
    </div>
  )
}

function Typing() {
  return (
    <div className="message">
      <BrandMini />
      <div className="typing" aria-label="正在检索">
        <i />
        <i />
        <i />
      </div>
    </div>
  )
}

/** 会话消息流（复刻原型 `.message-list`）。 */
export function MessageList() {
  const messages = useKb((s) => s.current.messages)
  const busy = useKb((s) => s.busy)
  return (
    <div className="message-list" aria-live="polite">
      {messages.map((m) => (
        <MessageView key={m.id} m={m} />
      ))}
      {busy && <Typing />}
    </div>
  )
}
