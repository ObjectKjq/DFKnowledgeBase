import { Fragment } from 'react'

import { Icon } from '@/components/Icon'
import { useLibraries } from '@/features/knowledge/useLibraries'
import { libName } from '@/mock/logic'
import { useChat } from '@/stores/chat'
import { useKb } from '@/stores/knowledge'

import { sendQuestion } from './chatActions'
import { useConversation, type ChatMessage, type MessageContent } from './conversations'

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

function MessageView({ message }: { message: ChatMessage }) {
  const { libs } = useLibraries()
  const showPreview = useChat((s) => s.showPreview)
  const content = message.content
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
      {message.type === 'results' ? (
        <>
          <p className="result-summary">混合检索 · 找到 {content.results?.length ?? 0} 个分块</p>
          {content.results?.map((chunk) => (
            <button
              className="result-card"
              key={chunk.chunkId}
              disabled={chunk.chunkId === undefined}
              onClick={() => showPreview(chunk.chunkId!)}
            >
              <span className="result-top">
                <h3>
                  <Icon name="file" />
                  {chunk.docTitle}
                </h3>
                <span className="tag">混合检索</span>
              </span>
              <span className="result-snippet chat-plain-text">{chunk.text}</span>
              <span className="result-path">
                {chunk.knowledgeBaseName} › {chunk.pageNumber ? `第 ${chunk.pageNumber} 页 › ` : ''}
                分块 {(chunk.chunkIndex ?? 0) + 1}
              </span>
            </button>
          ))}
        </>
      ) : message.type === 'answer' || message.type === 'empty' ? (
        <Answer content={content} />
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
      {visible.map((message) => (
        <MessageView key={message.id} message={message} />
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
            {currentPending.error && !currentPending.completed ? (
              <div className="empty-result" role="alert">
                <h3>提问未完成</h3>
                <p>{currentPending.error}</p>
                <button
                  className="btn ghost"
                  disabled={busy}
                  onClick={() => void sendQuestion(currentPending.question)}
                >
                  重新提问
                </button>
              </div>
            ) : currentPending.mode === 'RAG 检索' &&
              (currentPending.text || currentPending.result) ? (
              <Answer
                provisional={!currentPending.result}
                content={
                  currentPending.result ?? {
                    answer: currentPending.text,
                    citations: currentPending.citations,
                  }
                }
              />
            ) : currentPending.results ? (
              <MessageView
                message={{
                  type: currentPending.results.length ? 'results' : 'empty',
                  content: {
                    results: currentPending.results,
                    answer: currentPending.results.length
                      ? undefined
                      : '当前范围内未找到相关资料，请调整范围或补充文档。',
                  },
                }}
              />
            ) : (
              <div className="typing" role="status" aria-label="正在检索与生成">
                <i />
                <i />
                <i />
              </div>
            )}
            {currentPending.completed && currentPending.error && (
              <div className="info-banner" role="alert">
                <span>{currentPending.error}</span>
                <button
                  disabled={busy}
                  className="text-btn"
                  onClick={() =>
                    void query.refetch().then((result) => {
                      if (result.isSuccess) useChat.setState({ pending: null })
                    })
                  }
                >
                  刷新历史
                </button>
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
