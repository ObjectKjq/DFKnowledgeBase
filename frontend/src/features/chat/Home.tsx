import { useLibraries } from '@/features/knowledge/useLibraries'
import { useNavigate } from 'react-router-dom'

import { HERO_ART, SUGGESTION_SETS } from '@/mock/data'
import { libName } from '@/mock/logic'
import { useKb } from '@/stores/knowledge'

import { Icon } from '@/components/Icon'

/** 首屏空态（复刻原型 homeHTML）。 */
export function Home() {
  const docs = useKb((s) => s.docs)
  const { libs } = useLibraries()
  const suggestionIndex = useKb((s) => s.suggestionIndex)
  const shuffleSuggestions = useKb((s) => s.shuffleSuggestions)
  const openUpload = useKb((s) => s.openUpload)
  const showPreview = useKb((s) => s.showPreview)
  const navigate = useNavigate()

  const recent = docs
    .filter((d) => d.status === '可用' && libs.some((library) => library.id === d.lib))
    .slice(0, 3)
  const suggestions = SUGGESTION_SETS[suggestionIndex % 2]

  const startImageSearch = () => {
    const s = useKb.getState()
    if (!s.current.skills.includes('图片搜索')) s.toggleSkill('图片搜索')
    s.setDraft('')
    document.getElementById('query')?.focus()
    s.toastMsg('已开启图片搜索：输入描述，或从「＋」选择图片')
  }

  const suggest = (q: string, mode: string, image: boolean) => {
    const s = useKb.getState()
    s.setMode(mode)
    if (image && !s.current.skills.includes('图片搜索')) s.toggleSkill('图片搜索')
    void s.sendQuery(q)
  }

  return (
    <div className="home">
      <div className="hero">
        <div dangerouslySetInnerHTML={{ __html: HERO_ART }} />
        <p className="eyebrow">A QUIET PLACE FOR YOUR KNOWLEDGE</p>
        <h1>
          今天想找<span>什么？</span>
        </h1>
        <p className="hero-description">
          让散落的资料，有序安放。
          <br />
          从一次搜索开始，与积累的知识重新相遇。
        </p>
      </div>
      <div className="quick-actions">
        <button className="quick-card" onClick={openUpload}>
          <span className="quick-icon">
            <Icon name="upload" />
          </span>
          <Icon name="arrow" className="arrow" />
          <h2>上传你的资料</h2>
          <p>
            文档、笔记与灵感
            <br />
            在这里，慢慢积累。
          </p>
        </button>
        <button className="quick-card" onClick={startImageSearch}>
          <span className="quick-icon">
            <Icon name="image" />
          </span>
          <Icon name="arrow" className="arrow" />
          <h2>试试图片搜索</h2>
          <p>
            一段描述，或一张图片
            <br />
            找回记忆中的画面。
          </p>
        </button>
        <button className="quick-card" onClick={() => navigate('/settings/mcp')}>
          <span className="quick-icon">
            <Icon name="plug" />
          </span>
          <Icon name="arrow" className="arrow" />
          <h2>连接 AI 智能体</h2>
          <p>
            将你的知识带入
            <br />
            Codex、Trae 和更多工具。
          </p>
        </button>
      </div>
      <div className="suggestions">
        <div className="micro-heading">
          <span>从一个好问题开始</span>
          <button className="icon-btn" onClick={shuffleSuggestions} aria-label="换一组问题">
            <Icon name="refresh" />
          </button>
        </div>
        <div className="suggestion-list">
          {suggestions.map((q, i) => {
            const image = q.includes('图片') || q.includes('参考图')
            return (
              <button
                className="suggestion"
                key={q}
                onClick={() => suggest(q, i === 1 ? 'RAG 检索' : '混合检索', image)}
              >
                <Icon name={i === 2 ? 'image' : i === 1 ? 'spark' : 'search'} />
                {q}
              </button>
            )
          })}
        </div>
      </div>
      <div className="recent-docs">
        <div className="micro-heading">
          <span>
            最近加入<span style={{ color: '#aab19f' }}> · 随时可寻</span>
          </span>
          <button className="text-btn" onClick={() => navigate('/settings/docs')}>
            查看全部
            <Icon name="arrow" />
          </button>
        </div>
        <div className="recent-doc-list">
          {recent.map((d) => (
            <button className="recent-doc" key={d.id} onClick={() => showPreview(d.id)}>
              <span className={`file-icon ${d.ext}`}>
                <Icon name={d.ext === 'png' ? 'image' : 'file'} />
              </span>
              <div>
                <strong>{d.title}</strong>
                <small>
                  {libName(libs, d.lib)} · {d.date.slice(5).replace('-', '月')}日
                </small>
              </div>
            </button>
          ))}
        </div>
      </div>
    </div>
  )
}
