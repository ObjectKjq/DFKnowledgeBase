import { useEffect, useRef, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { simpleList } from '@/api/modelConfigurationController'
import { useAuthStore } from '@/stores/auth'

import { Icon } from '@/components/Icon'
import { DOCUMENT_FORMATS } from '@/features/documents/documents'
import { useLibraries } from '@/features/knowledge/useLibraries'
import { libName } from '@/lib/format'
import { useChat, type ChatMode } from '@/stores/chat'
import { useKb } from '@/stores/knowledge'

import { sendQuestion, stopQuestion } from './chatActions'
import { useConversation } from './conversations'
import { currentSelection, saveSelection } from './selection'

const MODES: [ChatMode, string][] = [
  ['混合检索', '关键字与语义融合，直接查看命中分块'],
  ['模糊检索', '仅按语义匹配，适合凭模糊记忆查找'],
  ['引导检索', '最多澄清两轮，也可直接混合检索'],
  ['RAG 检索', '基于文档生成带引用的流式答案'],
]

/** 输入选项仅呈现后端已支持的检索模式和知识库范围。 */
function Popover() {
  const filters = useChat((s) => s.filters)
  const setFilters = useChat((s) => s.setFilters)
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
      {popover === 'filters' && (
        <>
          <div className="popover-title">过滤条件 · 与知识库范围叠加</div>
          {Object.entries(DOCUMENT_FORMATS).map(([value, label]) => (
            <label className="menu-option" key={value}>
              <input
                type="checkbox"
                checked={filters.formats.includes(Number(value))}
                onChange={(e) =>
                  setFilters({
                    ...filters,
                    formats: e.target.checked
                      ? [...filters.formats, Number(value)]
                      : filters.formats.filter((format) => format !== Number(value)),
                  })
                }
              />
              {label}
            </label>
          ))}
          <label className="field">
            上传起点
            <input
              type="datetime-local"
              value={filters.uploadedFrom ?? ''}
              onChange={(e) =>
                setFilters({ ...filters, uploadedFrom: e.target.value || undefined })
              }
            />
          </label>
          <label className="field">
            上传终点
            <input
              type="datetime-local"
              value={filters.uploadedTo ?? ''}
              onChange={(e) => setFilters({ ...filters, uploadedTo: e.target.value || undefined })}
            />
          </label>
          <button className="text-btn" onClick={() => setFilters({ formats: [] })}>
            清空过滤
          </button>
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
  const selectedChunkIds = useChat((s) => s.selectedChunkIds)
  const clearChunks = useChat((s) => s.clearChunks)
  const token = useAuthStore((s) => s.token)
  const modelId = useChat((s) => s.modelId)
  const setModelId = useChat((s) => s.setModelId)
  const models = useQuery({
    queryKey: ['chat-models', token],
    queryFn: async () => (await simpleList()).data ?? [],
    enabled: !!token,
  })
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
  const [selectionError, setSelectionError] = useState('')
  const filters = useChat((s) => s.filters)
  const selection = JSON.stringify({ ...currentSelection(), formats: filters.formats })
  useEffect(() => {
    if (id === null || restoreScope || busy || !token) return
    let active = true
    void saveSelection(id, JSON.parse(selection) as API.RetrievalState)
      .then(() => {
        if (active) setSelectionError('')
      })
      .catch((error: unknown) => {
        if (active) setSelectionError(error instanceof Error ? error.message : '选择保存失败')
      })
    return () => {
      active = false
    }
  }, [id, restoreScope, busy, selection, token])
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
        {selectionError && (
          <div role="alert">
            选择尚未保存：{selectionError}
            <button
              onClick={() =>
                id !== null &&
                void saveSelection(id, currentSelection())
                  .then(() => setSelectionError(''))
                  .catch((error: unknown) =>
                    setSelectionError(error instanceof Error ? error.message : '选择保存失败'),
                  )
              }
            >
              重试保存选择
            </button>
          </div>
        )}
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
            {!!selectedChunkIds.length && (
              <span className="chip">
                所选上下文：{selectedChunkIds.length} 个分块
                <button type="button" aria-label="清空所选上下文" onClick={clearChunks}>
                  清空
                </button>
              </span>
            )}
            <label className="chip">
              对话模型
              <select
                aria-label="对话模型"
                value={modelId ?? ''}
                onChange={(e) => setModelId(e.target.value ? Number(e.target.value) : undefined)}
              >
                <option value="">选择对话模型</option>
                {models.data?.map((model) => (
                  <option key={model.id} value={model.id}>
                    {model.name}
                    {model.simulated ? '（本地模拟）' : ''}
                  </option>
                ))}
              </select>
            </label>
            {models.isSuccess && !models.data.length && (
              <span className="table-note">未配置对话模型，请联系管理员</span>
            )}
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
            className="query"
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
                className="text-btn"
                aria-label="过滤条件"
                onClick={() => openPopover('filters')}
              >
                过滤条件
              </button>
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
