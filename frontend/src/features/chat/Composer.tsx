import { useEffect, useRef, useState } from 'react'

import { Icon } from '@/components/Icon'
import { useLibraries } from '@/features/knowledge/useLibraries'
import { libName } from '@/mock/logic'
import { useChat, type ChatMode } from '@/stores/chat'
import { useKb } from '@/stores/knowledge'

import { sendQuestion, stopQuestion } from './chatActions'
import { useConversation } from './conversations'

const MODES: [ChatMode, string][] = [
  ['混合检索', '关键字与语义融合，直接查看命中分块'],
  ['RAG 检索', '基于文档生成带引用的流式答案'],
]

/** 输入选项仅呈现后端已支持的检索模式和知识库范围。 */
function Popover() {
  const popover = useKb((s) => s.popover)
  const close = useKb((s) => s.closePopover)
  const { libs } = useLibraries()
  const scope = useChat((s) => s.scope)
  const setScope = useChat((s) => s.setScope)
  const mode = useChat((s) => s.mode)
  const setMode = useChat((s) => s.setMode)
  const openUpload = useKb((s) => s.openUpload)
  if (!popover) return null
  return (
    <div className={`popover open ${popover}`} id="popover" role="dialog" aria-label="输入选项">
      {popover === 'scope' && (
        <>
          <div className="popover-title">检索范围 · 支持多选</div>
          <label className="menu-option">
            <input type="checkbox" checked={!scope.length} onChange={() => setScope([])} />
            全部有权访问的知识库
          </label>
          {libs.map((library) => (
            <label className="menu-option" key={library.id}>
              <input
                type="checkbox"
                checked={scope.includes(library.id)}
                onChange={(e) =>
                  setScope(
                    e.target.checked
                      ? [...scope, library.id]
                      : scope.filter((id) => id !== library.id),
                  )
                }
              />
              {library.name}
              <span className="description">{library.role}</span>
            </label>
          ))}
          <div className="menu-footer">仅影响后续提问，已发送的提问和引用保持原有范围。</div>
        </>
      )}
      {popover === 'mode' && (
        <>
          <div className="popover-title">检索方式</div>
          {MODES.map(([value, description]) => (
            <button
              className={`menu-option ${mode === value ? 'active' : ''}`}
              key={value}
              onClick={() => {
                setMode(value)
                close()
              }}
            >
              <Icon name={value === 'RAG 检索' ? 'spark' : 'search'} />
              <span>
                {value}
                <span className="description">{description}</span>
              </span>
            </button>
          ))}
        </>
      )}
      {popover === 'add' && (
        <button
          className="menu-option"
          onClick={() => {
            close()
            openUpload()
          }}
        >
          <Icon name="upload" />
          <span>
            上传文档<span className="description">PDF、Office、TXT 与 Markdown</span>
          </span>
        </button>
      )}
    </div>
  )
}

/** 聊天输入区：发送时固定范围，支持文档拖拽与停止生成。 */
export function Composer() {
  const id = useChat((s) => s.conversationId)
  const draft = useChat((s) => s.draft)
  const setDraft = useChat((s) => s.setDraft)
  const busy = useChat((s) => s.busy)
  const restoreScope = useChat((s) => s.restoreScope)
  const scope = useChat((s) => s.scope)
  const mode = useChat((s) => s.mode)
  const openPopover = useKb((s) => s.openPopover)
  const closePopover = useKb((s) => s.closePopover)
  const popover = useKb((s) => s.popover)
  const { libs } = useLibraries()
  const conversation = useConversation()
  const textarea = useRef<HTMLTextAreaElement>(null)
  const [dragging, setDragging] = useState(false)
  useEffect(() => {
    if (!draft) textarea.current?.focus()
  }, [draft, id])
  const label =
    scope.length === 1
      ? libName(libs, scope[0])
      : scope.length
        ? `${scope.length} 个知识库`
        : '全部知识库'
  const send = () => {
    closePopover()
    void sendQuestion()
  }
  const unavailable =
    restoreScope || (id !== null && (conversation.isPending || conversation.isError))
  return (
    <div className="composer-area">
      <div className="composer-wrap">
        <Popover />
        <form
          className={`composer ${dragging ? 'dragging' : ''}`}
          onSubmit={(e) => {
            e.preventDefault()
            if (!unavailable) send()
          }}
          onDragOver={(e) => {
            e.preventDefault()
            setDragging(true)
          }}
          onDragLeave={() => setDragging(false)}
          onDrop={(e) => {
            e.preventDefault()
            setDragging(false)
            if (!e.dataTransfer.files.length) return
            useKb.getState().openUpload()
            useKb.getState().addPendingFiles([...e.dataTransfer.files])
          }}
        >
          <div className="chips">
            <button
              type="button"
              className="chip"
              disabled={restoreScope}
              onClick={() => openPopover('scope')}
              data-menu="scope"
              aria-label={`检索范围：${label}`}
              aria-expanded={popover === 'scope'}
            >
              <Icon name="layers" />
              {label}
              <Icon name="down" />
            </button>
          </div>
          <textarea
            ref={textarea}
            id="query"
            aria-label="检索问题"
            placeholder={
              mode === 'RAG 检索'
                ? '向你的文档提问，核对每一处引用…'
                : '搜索你的知识，查看命中原文…'
            }
            maxLength={2000}
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' && !e.shiftKey && !e.nativeEvent.isComposing) {
                e.preventDefault()
                if (!unavailable) send()
              }
            }}
          />
          <div className="composer-bottom">
            <div className="composer-left">
              <button
                type="button"
                className="add-btn"
                onClick={() => openPopover('add')}
                data-menu="add"
                aria-label="添加文档"
                aria-expanded={popover === 'add'}
              >
                <Icon name="plus" />
              </button>
            </div>
            <div className="composer-right">
              <button
                type="button"
                className="text-btn mode-btn"
                onClick={() => openPopover('mode')}
                data-menu="mode"
                aria-expanded={popover === 'mode'}
              >
                <Icon name="search" />
                {mode}
                <Icon name="down" />
              </button>
              {busy ? (
                <button type="button" className="btn small" onClick={stopQuestion}>
                  停止生成
                </button>
              ) : (
                <button
                  type="submit"
                  className="send"
                  aria-label="发送检索"
                  disabled={!draft.trim() || unavailable}
                >
                  <Icon name="up" />
                </button>
              )}
            </div>
          </div>
        </form>
        <div className="composer-caption">
          <span>
            <Icon name="lock" />
            仅检索你有权访问的知识库；模型由实例配置决定。
          </span>
          <span>Enter 发送 · Shift + Enter 换行</span>
        </div>
      </div>
    </div>
  )
}
