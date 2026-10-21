import { useLibraries } from '@/features/knowledge/useLibraries'
import { useEffect, useRef, useState } from 'react'

import { MODES } from '@/mock/data'
import { libName } from '@/mock/logic'
import { useKb } from '@/stores/knowledge'

import { Icon } from '@/components/Icon'

const SKILLS: [string, string][] = [
  ['联网搜索', '单独展示网络补充，不计入本地引用'],
  ['图片搜索', '图内文字、以文搜图与以图搜图'],
]

/** 对话模型下拉可选项（复刻原型 chatModelOptions）。 */
function chatModelOptions(chatName: string, chatType: string) {
  return [
    ...new Set([
      chatName + ' · ' + (chatType === '本地模型服务' ? '本地' : '私有服务'),
      'Qwen3 · 本地',
      'DeepSeek · 私有服务',
    ]),
  ]
}

function Popover() {
  const popover = useKb((s) => s.popover)
  const { libs } = useLibraries()
  const current = useKb((s) => s.current)
  const modelConfig = useKb((s) => s.modelConfig)
  const setScope = useKb((s) => s.setScope)
  const toggleScope = useKb((s) => s.toggleScope)
  const setMode = useKb((s) => s.setMode)
  const setModel = useKb((s) => s.setModel)
  const setTier = useKb((s) => s.setTier)
  const toggleSkill = useKb((s) => s.toggleSkill)
  const openPopover = useKb((s) => s.openPopover)

  if (!popover) return <div className="popover" id="popover" role="dialog" aria-label="输入选项" />

  return (
    <div className={`popover open ${popover}`} id="popover" role="dialog" aria-label="输入选项">
      {popover === 'scope' && (
        <>
          <div className="popover-title">检索范围 · 支持多选</div>
          <div className="menu-option">
            <input
              type="checkbox"
              id="scope-all"
              checked={!current.scope.length}
              onChange={() => {
                setScope([])
                openPopover('scope')
              }}
            />
            <label htmlFor="scope-all">全部有权访问的知识库</label>
          </div>
          {libs.map((l) => (
            <div className="menu-option" key={l.id}>
              <input
                type="checkbox"
                id={`scope-${l.id}`}
                checked={current.scope.includes(l.id)}
                onChange={(e) => {
                  toggleScope(l.id, e.target.checked)
                  openPopover('scope')
                }}
              />
              <label htmlFor={`scope-${l.id}`}>
                {l.name}
                <span className="description">{l.type === 'personal' ? '个人知识库' : l.role}</span>
              </label>
            </div>
          ))}
          <div className="menu-footer">更改仅对后续消息生效，已发送结果保持不变。</div>
        </>
      )}

      {popover === 'mode' && (
        <>
          <div className="popover-title">选择检索方式</div>
          {MODES.map(([m, d]) => (
            <button
              type="button"
              key={m}
              className={`menu-option ${current.mode === m ? 'active' : ''}`}
              onClick={() => setMode(m)}
            >
              <Icon name={m === 'RAG 检索' ? 'spark' : 'search'} />
              <span>
                {m}
                <span className="description">{d}</span>
              </span>
              {current.mode === m && <Icon name="check" className="tick" />}
            </button>
          ))}
        </>
      )}

      {popover === 'model' && (
        <>
          <div className="popover-title">对话模型</div>
          {chatModelOptions(modelConfig.chat.name, modelConfig.chat.type).map((m) => (
            <button
              type="button"
              key={m}
              className={`menu-option ${current.model === m ? 'active' : ''}`}
              onClick={() => setModel(m)}
            >
              <Icon name="spark" />
              <span>
                {m}
                <span className="description">示例模型 · 未接入实际服务</span>
              </span>
              {current.model === m && <Icon name="check" className="tick" />}
            </button>
          ))}
          <div className="model-tier">
            {['轻快', '均衡', '深度'].map((t) => (
              <button
                type="button"
                key={t}
                className={current.tier === t ? 'active' : ''}
                onClick={() => setTier(t)}
              >
                {t}
              </button>
            ))}
          </div>
          <div className="menu-footer">向量与重排模型在「实例设置」中配置。</div>
        </>
      )}

      {popover === 'add' && (
        <>
          <button
            type="button"
            className="menu-option"
            onClick={() => useKb.getState().openUpload()}
          >
            <Icon name="upload" />
            <span>
              上传资料
              <span className="description">PDF、Office、TXT 与 Markdown</span>
            </span>
          </button>
          <button
            type="button"
            className="menu-option"
            onClick={() => {
              document.getElementById('image-input')?.click()
              useKb.getState().closePopover()
            }}
          >
            <Icon name="image" />
            <span>
              以图搜图
              <span className="description">选择图片，查找相似内容</span>
            </span>
          </button>
        </>
      )}

      {popover === 'skills' && (
        <>
          <div className="popover-title">为本次检索添加能力</div>
          {SKILLS.map(([s, d]) => (
            <button
              type="button"
              key={s}
              className={`menu-option ${current.skills.includes(s) ? 'active' : ''}`}
              onClick={() => {
                toggleSkill(s)
                openPopover('skills')
              }}
            >
              <Icon name={s === '联网搜索' ? 'globe' : 'image'} />
              <span>
                {s}
                <span className="description">{d}</span>
              </span>
              <Icon name={current.skills.includes(s) ? 'check' : 'plus'} className="tick" />
            </button>
          ))}
          <button type="button" className="menu-option" disabled>
            <Icon name="spark" />
            <span>
              深度检索 <span className="tag oat">P1</span>
              <span className="description">多步拆解与跨文档归纳 · 后续开放</span>
            </span>
          </button>
        </>
      )}
    </div>
  )
}

/** 底部输入区（复刻原型 `.composer-area` / renderComposer）。 */
export function Composer() {
  const current = useKb((s) => s.current)
  const draft = useKb((s) => s.draft)
  const busy = useKb((s) => s.busy)
  const attachment = useKb((s) => s.attachment)
  const { libs } = useLibraries()
  const popover = useKb((s) => s.popover)
  const setDraft = useKb((s) => s.setDraft)
  const sendQuery = useKb((s) => s.sendQuery)
  const openPopover = useKb((s) => s.openPopover)
  const removeSkill = useKb((s) => s.removeSkill)
  const textarea = useRef<HTMLTextAreaElement>(null)
  const [dragging, setDragging] = useState(false)

  // 复刻原型：发送后清空并聚焦；此处随草稿变化自动聚焦。
  useEffect(() => {
    if (!draft) textarea.current?.focus()
  }, [current.id, draft])

  const scope =
    current.scope.length === 1
      ? libName(libs, current.scope[0])
      : current.scope.length > 1
        ? `${current.scope.length} 个知识库`
        : '全局检索'
  const placeholder =
    current.mode === 'RAG 检索'
      ? '向你的资料提问，每一份答案都有出处…'
      : current.skills.includes('图片搜索')
        ? '描述你想找的图片，或拖入一张图片…'
        : '搜索你的知识，或从一个问题开始…'
  const caption = current.skills.includes('联网搜索')
    ? '网络补充单独展示，不作为本地答案引用。当前为离线演示。'
    : '你的资料，只属于你。示例检索在浏览器本地运行。'

  return (
    <div className="composer-area">
      <div className="composer-wrap">
        <Popover />
        <form
          className={`composer ${dragging ? 'dragging' : ''}`}
          id="composer"
          onSubmit={(e) => {
            e.preventDefault()
            void sendQuery()
          }}
          onDragOver={(e) => {
            e.preventDefault()
            setDragging(true)
          }}
          onDragLeave={() => setDragging(false)}
          onDrop={(e) => {
            e.preventDefault()
            setDragging(false)
            const file = e.dataTransfer.files[0]
            if (file?.type.startsWith('image/')) {
              useKb.getState().setImage(file)
              return
            }
            if (!file) return
            useKb.getState().openUpload()
            useKb.getState().addPendingFiles([...e.dataTransfer.files])
          }}
        >
          <div className="chips" id="chips">
            <button
              type="button"
              className="chip"
              onClick={() => openPopover('scope')}
              data-menu="scope"
              aria-expanded={popover === 'scope'}
            >
              <Icon name="layers" />
              {scope}
              <Icon name="down" />
            </button>
            {current.skills.map((s) => (
              <button
                type="button"
                key={s}
                className="chip skill-chip"
                onClick={() => removeSkill(s)}
                aria-label={`关闭${s}`}
              >
                <Icon name={s === '联网搜索' ? 'globe' : 'image'} />
                {s}
                <Icon name="close" />
              </button>
            ))}
          </div>
          <div id="attachment">
            {attachment && (
              <div className="attachment">
                {attachment.dataUrl ? (
                  <img src={attachment.dataUrl} alt="查询图片缩略图" />
                ) : (
                  <Icon name="file" />
                )}
                <span>{attachment.name}</span>
                <button
                  type="button"
                  onClick={() => useKb.getState().setAttachment(null)}
                  aria-label="移除查询图片"
                >
                  <Icon name="close" />
                </button>
              </div>
            )}
          </div>
          <label htmlFor="query" className="sr-only">
            输入检索问题
          </label>
          <textarea
            id="query"
            className="query"
            ref={textarea}
            rows={2}
            placeholder={placeholder}
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault()
                void sendQuery()
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
                aria-label="添加附件"
                aria-expanded={popover === 'add'}
              >
                <Icon name="plus" />
              </button>
              <button
                type="button"
                className="text-btn"
                onClick={() => openPopover('skills')}
                data-menu="skills"
                aria-expanded={popover === 'skills'}
              >
                <Icon name="spark" />
                技能
                <Icon name="down" />
              </button>
            </div>
            <div className="composer-right">
              <button
                type="button"
                className="text-btn mode-btn"
                id="mode-button"
                onClick={() => openPopover('mode')}
                data-menu="mode"
                aria-expanded={popover === 'mode'}
              >
                <Icon name="search" />
                {current.mode}
                <Icon name="down" />
              </button>
              <span className="vline" />
              <button
                type="button"
                className="text-btn model-btn"
                id="model-button"
                onClick={() => openPopover('model')}
                data-menu="model"
                aria-expanded={popover === 'model'}
              >
                {current.model}
                <span className="model-tier-name">· {current.tier}</span>
                <Icon name="down" />
              </button>
              <button
                type="submit"
                className="send"
                id="send-button"
                aria-label="发送检索"
                disabled={busy || (!draft.trim() && !attachment)}
              >
                <Icon name="up" />
              </button>
            </div>
          </div>
        </form>
        <div className="composer-caption">
          <span>
            <Icon name="lock" />
            <span id="privacy-caption">{caption}</span>
          </span>
          <span>
            <kbd>Enter</kbd> 发送 · <kbd>Shift + Enter</kbd> 换行
          </span>
        </div>
      </div>
    </div>
  )
}
