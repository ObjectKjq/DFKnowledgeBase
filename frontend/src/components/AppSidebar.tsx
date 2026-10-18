import { useNavigate } from 'react-router-dom'

import { menuRoute } from '@/features/manage/tabs'
import { useAuthStore } from '@/stores/auth'
import { useKb } from '@/stores/knowledge'

import { Icon } from './Icon'

const DEMO_HISTORY: [string, string][] = [
  ['demo-rag', '如何让知识检索更可信？'],
  ['demo-design', '关于渐进式披露的设计原则'],
  ['demo-image', '找一张极简设计的参考图'],
]

/** 左侧主导航（复刻原型 `.sidebar`）。 */
export function AppSidebar({ open, onClose }: { open: boolean; onClose: () => void }) {
  const navigate = useNavigate()
  const libs = useKb((s) => s.libs)
  const docs = useKb((s) => s.docs)
  const sessions = useKb((s) => s.sessions)
  const menus = useAuthStore((s) => s.menus)
  const scope = useKb((s) => s.current.scope)
  const currentId = useKb((s) => s.current.id)
  const setScope = useKb((s) => s.setScope)
  const openHistory = useKb((s) => s.openHistory)
  const newChat = useKb((s) => s.newChat)
  const openModal = useKb((s) => s.openModal)

  const available = (libId?: string) =>
    docs.filter((d) => d.status === '可用' && (!libId || d.lib === libId)).length

  const goScope = (id?: string) => {
    setScope(id ? [id] : [])
    navigate('/')
    onClose()
  }

  const histItems: [string, string][] = [
    ...sessions.slice(0, 5).map((s) => [s.id, s.title] as [string, string]),
    ...DEMO_HISTORY.filter((d) => !sessions.some((s) => s.id === d[0] || s.title === d[1])),
  ].slice(0, 5)

  const personalLibs = libs.filter((l) => l.type === 'personal')
  const sharedLibs = libs.filter((l) => l.type === 'shared')

  return (
    <aside className={`sidebar ${open ? 'open' : ''}`} aria-label="主导航">
      <div className="brand">
        <svg className="brand-mark" viewBox="0 0 36 40" aria-hidden="true">
          <path
            d="M17 32V12C11 7 5 8 3 9v21c6-1 11 0 14 3 4-3 9-4 15-3V9c-3-1-9-1-15 3"
            strokeWidth="1.5"
          />
          <path
            d="M20 8c0-5 5-7 9-6-1 5-4 8-9 6ZM9 16l4 2m-4 3 4 2m9-7 5-1m-5 6 5-1"
            strokeWidth="1.2"
          />
        </svg>
        <div>
          <div className="brand-name">知序</div>
          <div className="brand-en">KNOWLEDGE, AT HOME.</div>
        </div>
      </div>

      <button
        className="new-search"
        onClick={() => {
          newChat()
          navigate('/')
          onClose()
        }}
      >
        <Icon name="plus" />
        新检索<span className="shortcut-key">Ctrl K</span>
      </button>

      <div className="side-scroll">
        <div className="section-label">
          知识空间
          <button aria-label="新建共享知识库" onClick={() => openModal({ kind: 'library' })}>
            <Icon name="plus" />
          </button>
        </div>
        <nav aria-label="知识库">
          <button className={`nav-item ${!scope.length ? 'active' : ''}`} onClick={() => goScope()}>
            <Icon name="layers" />
            <span>全部知识库</span>
            <span className="count">{available()}</span>
          </button>
          {personalLibs.map((l) => (
            <button
              key={l.id}
              className={`nav-item ${scope.length === 1 && scope[0] === l.id ? 'active' : ''}`}
              onClick={() => goScope(l.id)}
            >
              <Icon name="book" />
              <span>{l.name}</span>
              <span className="count">{available(l.id)}</span>
            </button>
          ))}
          <div className="side-subtitle">共享知识库</div>
          {sharedLibs.map((l) => (
            <button
              key={l.id}
              className={`nav-item ${scope.length === 1 && scope[0] === l.id ? 'active' : ''}`}
              onClick={() => goScope(l.id)}
            >
              <Icon name="users" />
              <span>{l.name}</span>
              <span className="count">{available(l.id)}</span>
            </button>
          ))}
        </nav>

        <div className="section-label history-label">
          最近会话
          <Icon name="refresh" />
        </div>
        <nav aria-label="最近会话">
          {histItems.map(([id, title]) => (
            <button
              key={id}
              className={`history-item ${currentId === id ? 'active' : ''}`}
              title={title}
              onClick={() => {
                openHistory(id)
                navigate('/')
                onClose()
              }}
            >
              {title}
            </button>
          ))}
        </nav>

        {/* 功能入口由登录返回的菜单树驱动，无权的菜单不渲染。 */}
        {menus.length > 0 && (
          <>
            <div className="section-label">功能菜单</div>
            <nav aria-label="功能菜单">
              {menus.map((m) =>
                m.type === 1 ? (
                  <div key={m.id}>
                    <div className="side-subtitle">{m.name}</div>
                    {((m.children ?? []) as API.MenuVO[]).map((c) => (
                      <button
                        key={c.id}
                        className="nav-item"
                        onClick={() => {
                          navigate(menuRoute(c.path))
                          onClose()
                        }}
                      >
                        <Icon name="settings" />
                        <span>{c.name}</span>
                      </button>
                    ))}
                  </div>
                ) : (
                  <button
                    key={m.id}
                    className="nav-item"
                    onClick={() => {
                      navigate(menuRoute(m.path))
                      onClose()
                    }}
                  >
                    <Icon name="layers" />
                    <span>{m.name}</span>
                  </button>
                ),
              )}
            </nav>
          </>
        )}
      </div>

      <div className="side-bottom">
        <div className="local-note">
          <strong>
            <Icon name="shield" />
            知识在此，安心在此。
          </strong>
          <p>资料存于你的环境，灵感随时可寻。</p>
          <div className="storage-track">
            <span />
          </div>
        </div>
        <div className="settings-nav">
          <button
            onClick={() => {
              navigate('/settings/docs')
              onClose()
            }}
          >
            <Icon name="settings" />
            设置与管理
          </button>
          <small>v0.3</small>
        </div>
      </div>
    </aside>
  )
}
