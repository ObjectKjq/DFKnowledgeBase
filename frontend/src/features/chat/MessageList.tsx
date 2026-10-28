import { Fragment, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { search } from '@/api/docSearchController'
import { useAuthStore } from '@/stores/auth'

import { Icon } from '@/components/Icon'
import { useLibraries } from '@/features/knowledge/useLibraries'
import { libName } from '@/lib/format'
import { useChat } from '@/stores/chat'
import { useKb } from '@/stores/knowledge'

import { sendQuestion } from './chatActions'
import { useConversation, type ChatMessage, type MessageContent } from './conversations'

/** 高亮只解释后端 em 标记，其他内容始终作为文本渲染。 */
function HitSnippet({ chunk }: { chunk: API.DocChunkRespVO }) {
  if (!chunk.highlight) return <>{chunk.text}</>
  const decode = (value: string) =>
    value.replace(
      /&(lt|gt|amp|quot|#39);/g,
      (_, name: string) => ({ lt: '<', gt: '>', amp: '&', quot: '"', '#39': "'" })[name] ?? '',
    )
  return (
    <>
      {chunk.highlight
        .split(/(<em>.*?<\/em>)/gs)
        .map((part, i) =>
          part.startsWith('<em>') ? (
            <mark key={i}>{decode(part.slice(4, -5))}</mark>
          ) : (
            <Fragment key={i}>{decode(part)}</Fragment>
          ),
        )}
    </>
  )
}

function Citation({ citation }: { citation: API.RagCitationRespVO }) {
  const showPreview = useChat((s) => s.showPreview)
  return (
    <button
      className="citation-card"
      disabled={citation.chunkId === undefined}
      onClick={() => showPreview(citation.chunkId!)}
    >
      <b>{citation.number}</b>
      <div>
        <strong>{citation.docTitle}</strong>
        <small>
          {citation.knowledgeBaseName} ·{' '}
          {citation.pageNumber ? `第 ${citation.pageNumber} 页 · ` : ''}分块{' '}
          {(citation.chunkIndex ?? 0) + 1}
        </small>
      </div>
    </button>
  )
}

function Answer({
  content,
  provisional = false,
}: {
  content: MessageContent
  provisional?: boolean
}) {
  const showPreview = useChat((s) => s.showPreview)
  const copyText = useKb((s) => s.copyText)
  const citations = content.answered === false ? [] : (content.citations ?? [])
  return (
    <>
      <div className="answer-header">
        <span className="mini-mark">
          <Icon name="book" />
        </span>
        知序{provisional && <span className="hint-chip">生成中 · 暂定内容</span>}
      </div>
      {content.answered === false && <p className="table-note">依据不足，未生成答案</p>}
      <div className="answer-content chat-plain-text">
        {(content.answer ?? '').split(/(\[\d+\])/).map((part, index) => {
          const number = /^\[(\d+)\]$/.exec(part)?.[1]
          const citation = number
            ? citations.find((citation) => citation.number === Number(number))
            : undefined
          return citation?.chunkId !== undefined ? (
            <button
              className="inline-cite"
              key={index}
              onClick={() => showPreview(citation.chunkId!)}
              aria-label={`查看引用${number}`}
            >
              {number}
            </button>
          ) : (
            <Fragment key={index}>{part}</Fragment>
          )
        })}
      </div>
      {!!citations.length && (
        <>
          <p className="sources-title">引用来源 · 点击核对原文</p>
          <div className="citation-grid">
            {citations.map((citation) => (
              <Citation key={`${citation.number}-${citation.chunkId}`} citation={citation} />
            ))}
          </div>
        </>
      )}
      {!!content.suggestions?.length && (
        <ul className="table-note">
          {content.suggestions.map((suggestion) => (
            <li key={suggestion}>{suggestion}</li>
          ))}
        </ul>
      )}
      {!provisional && content.answer && (
        <div className="response-meta">
          <button onClick={() => void copyText(content.answer!)} aria-label="复制回答">
            <Icon name="copy" />
          </button>
        </div>
      )}
    </>
  )
}

function MessageView({
  message,
  activeGuide = false,
}: {
  message: ChatMessage
  activeGuide?: boolean
}) {
  const busy = useChat((s) => s.busy)
  const guideQuestion = useChat((s) => s.guideState.guideQuestion)
  const token = useAuthStore((s) => s.token)
  const original = message.content
  const [pageNo, setPageNo] = useState(original.pageNo ?? 1)
  const page = useQuery({
    queryKey: [
      'message-search-page',
      token,
      message.id,
      original.question,
      original.selection,
      pageNo,
    ],
    queryFn: async () =>
      (
        await search({
          query: original.question ?? '',
          ...original.selection,
          mode: original.mode === '模糊检索' ? 'FUZZY' : 'HYBRID',
          pageNo,
          pageSize: 20,
        })
      ).data,
    enabled: !!token && message.type === 'results' && pageNo !== (original.pageNo ?? 1),
    retry: false,
  })
  const selected = useChat((s) => s.selectedChunkIds)
  const toggle = useChat((s) => s.toggleChunk)
  const { libs } = useLibraries()
  const showPreview = useChat((s) => s.showPreview)
  const content =
    pageNo === (original.pageNo ?? 1)
      ? original
      : { ...original, results: page.data?.list ?? [], searchPage: page.data }
  if (message.type === 'user') {
    const scope =
      content.knowledgeBaseIds?.map((id) => libName(libs, String(id))).join('、') || '全部知识库'
    return (
      <div className="message message-user">
        <div className="user-bubble chat-plain-text">
          {content.text}
          <small>
            {scope} · {content.mode || 'RAG 检索'}
            {message.createTime && ` · ${message.createTime.replace('T', ' ')}`}
          </small>
        </div>
      </div>
    )
  }
  return (
    <div className="message">
      {message.type === 'guide' ? (
        <div className="settings-card">
          <p>引导澄清 · 第 {content.round ?? 1} / 2 轮</p>
          <h3>{content.question}</h3>
          <p>可选择推荐选项，或在输入框自由回答。</p>
          <div className="form-actions">
            {activeGuide &&
              content.options?.map((option) => (
                <button
                  className="btn ghost"
                  key={option}
                  disabled={!activeGuide || busy}
                  onClick={() => void sendQuestion(option)}
                >
                  {option}
                </button>
              ))}
            {activeGuide && (
              <button
                className="btn"
                disabled={!activeGuide || busy}
                onClick={() => void sendQuestion(guideQuestion ?? '', { skipGuide: true })}
              >
                跳过引导，直接混合检索
              </button>
            )}
          </div>
        </div>
      ) : message.type === 'results' ? (
        <>
          <p className="result-summary">
            {content.mode ?? '混合检索'} · 找到{' '}
            {content.searchPage?.total ?? content.results?.length ?? 0} 个分块
          </p>
          {content.reason && <p role="status">{content.reason}</p>}
          {!content.results?.length && !page.isFetching && !page.isError && (
            <p className="empty-result">
              当前范围内没有匹配结果，请调整范围、文档格式或上传时间，也可补充文档后重试。
            </p>
          )}
          {!!content.searchPage?.indexingCount && (
            <p role="status">{content.searchPage.indexingCount} 份文档正在入库，完成后可检索。</p>
          )}
          {content.searchPage?.candidateLimitReached && (
            <p className="table-note">结果已达到候选范围上限，可缩小知识库或过滤条件继续检索。</p>
          )}
          {page.isFetching && <p role="status">正在加载第 {pageNo} 页…</p>}
          {page.isError && <button onClick={() => void page.refetch()}>分页加载失败，重试</button>}
          {content.results?.map((chunk) => (
            <div key={chunk.chunkId}>
              <label className="menu-option">
                <input
                  type="checkbox"
                  aria-label={`选择${chunk.docTitle}分块${(chunk.chunkIndex ?? 0) + 1}`}
                  checked={chunk.chunkId !== undefined && selected.includes(chunk.chunkId)}
                  disabled={chunk.chunkId === undefined}
                  onChange={() => toggle(chunk.chunkId!)}
                />
                用于所选上下文
              </label>
              <button
                className="result-card"
                disabled={chunk.chunkId === undefined}
                onClick={() => showPreview(chunk.chunkId!)}
              >
                <span className="result-top">
                  <h3>
                    <Icon name="file" />
                    {chunk.docTitle}
                  </h3>
                  <span className="tag">
                    {(
                      { KEYWORD: '关键词命中', SEMANTIC: '语义命中', HYBRID: '混合命中' } as Record<
                        string,
                        string
                      >
                    )[chunk.matchType ?? ''] ??
                      content.mode ??
                      '混合检索'}
                  </span>
                </span>
                <span className="result-snippet chat-plain-text">
                  <HitSnippet chunk={chunk} />
                </span>
                {chunk.ocr && <span className="tag">OCR 文本</span>}
                {typeof chunk.score === 'number' && (
                  <span className="table-note">相关性 {chunk.score.toFixed(4)}</span>
                )}
                <span className="result-path">
                  {chunk.knowledgeBaseName} ›{' '}
                  {chunk.pageNumber ? `第 ${chunk.pageNumber} 页 › ` : ''}
                  {chunk.sheetName && `工作表 ${chunk.sheetName} › `}
                  分块 {(chunk.chunkIndex ?? 0) + 1}
                </span>
              </button>
            </div>
          ))}
          {!!original.searchPage?.total && (
            <div className="pager">
              <button
                disabled={pageNo <= 1 || page.isFetching}
                onClick={() => setPageNo(pageNo - 1)}
              >
                上一页
              </button>
              <span>
                {pageNo} / {Math.ceil(original.searchPage.total / 20)}
              </span>
              <button
                disabled={pageNo * 20 >= original.searchPage.total || page.isFetching}
                onClick={() => setPageNo(pageNo + 1)}
              >
                下一页
              </button>
            </div>
          )}
        </>
      ) : message.type === 'answer' || message.type === 'empty' ? (
        <>
          {!!content.searchPage?.indexingCount && (
            <p role="status">{content.searchPage.indexingCount} 份文档正在入库，完成后可检索。</p>
          )}
          <Answer content={content} />
        </>
      ) : (
        <p className="chat-plain-text">
          {content.text || content.answer || '该消息暂无可展示内容'}
        </p>
      )}
    </div>
  )
}

/** 服务器消息按时间渲染；当前提问的暂定内容在完整结果落库后移除。 */
export function MessageList() {
  const query = useConversation()
  const pending = useChat((s) => s.pending)
  const busy = useChat((s) => s.busy)
  const id = useChat((s) => s.conversationId)
  const messages = query.isError ? [] : (query.data?.messages ?? [])
  const currentPending = pending && pending.conversationId === id ? pending : null
  const visible = currentPending
    ? messages.filter((message) => (message.id ?? 0) <= currentPending.previousMessageId)
    : messages
  return (
    <div className="message-list" aria-live="polite">
      {id !== null && query.isPending && !currentPending && <p role="status">正在恢复会话…</p>}
      {query.isError && (
        <div className="empty-result" role="alert">
          <p>会话加载失败：{query.error.message}</p>
          <button className="btn ghost" onClick={() => void query.refetch()}>
            重新加载
          </button>
        </div>
      )}
      {visible.map((message, index) => (
        <MessageView
          key={message.id}
          message={message}
          activeGuide={!currentPending && index === visible.length - 1}
        />
      ))}
      {currentPending && (
        <>
          <MessageView
            message={{
              type: 'user',
              content: {
                text: currentPending.question,
                knowledgeBaseIds: currentPending.knowledgeBaseIds,
                mode: currentPending.mode,
              },
            }}
          />
          <div className="message">
            {currentPending.error ? (
              <div className="empty-result" role="alert">
                <h3>提问未完成</h3>
                <p>{currentPending.error}</p>
                <button
                  className="btn ghost"
                  disabled={busy}
                  onClick={() =>
                    void sendQuestion(currentPending.question, { retry: currentPending })
                  }
                >
                  重新提问
                </button>
              </div>
            ) : currentPending.mode === 'RAG 检索' && currentPending.text ? (
              <Answer
                provisional
                content={{ answer: currentPending.text, citations: currentPending.citations }}
              />
            ) : (
              <div className="typing" role="status" aria-label="正在检索与生成">
                <i />
                <i />
                <i />
              </div>
            )}
          </div>
        </>
      )}
      {!visible.length && !currentPending && query.isSuccess && (
        <p className="table-note">会话暂无消息，可以开始提问。</p>
      )}
    </div>
  )
}
