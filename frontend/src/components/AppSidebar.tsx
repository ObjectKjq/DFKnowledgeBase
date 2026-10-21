import { useMutation, useQueryClient } from '@tanstack/react-query'
import { deleteConversation } from '@/api/conversationController'
import { useConversations, conversationsKey } from '@/features/chat/conversations'
import { useChat } from '@/stores/chat'
import { useLibraries } from '@/features/knowledge/useLibraries'
import { useNavigate } from 'react-router-dom'

import { MENU_TYPE_DIR } from '@/features/manage/menuTree'
import { menuRoute } from '@/features/manage/tabs'
import { useAuthStore } from '@/stores/auth'
import { useKb } from '@/stores/knowledge'

import { Icon } from './Icon'

/** 左侧主导航（复刻原型 `.sidebar`）。 */
export function AppSidebar({ open, onClose }: { open: boolean; onClose: () => void }) {
  const navigate = useNavigate()
  const { libs, isPending, isError, refetch } = useLibraries()
  const menus = useAuthStore((s) => s.menus)
  const scope = useChat((s) => s.scope)
  const currentId = useChat((s) => s.conversationId)
  const setScope = useChat((s) => s.setScope)
  const newChat = useChat((s) => s.newChat)
  const openModal = useKb((s) => s.openModal)

  const goScope = (id?: string) => {
    setScope(id ? [id] : [])
    navigate('/')
    onClose()
  }

  const history = useConversations()
  const busy = useChat((s) => s.busy)
  const selectConversation = useChat((s) => s.selectConversation)
  const confirmThen = useKb((s) => s.confirmThen)
  const token = useAuthStore((s) => s.token)
  const client = useQueryClient()
  const deletion = useMutation({
    mutationFn: (id: number) => deleteConversation({ id }),
    onSuccess: (_, id) => {
      if (useChat.getState().conversationId === id) newChat()
      client.removeQueries({ queryKey: ['chat-conversation', token, id] })
      void client.invalidateQueries({ queryKey: conversationsKey(token) })
    },
  })

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
        disabled={busy}
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
          知识库
          <button aria-label="新建知识库" onClick={() => openModal({ kind: 'library' })}>
            <Icon name="plus" />
          </button>
        </div>
        {isPending && <p className="table-note">正在加载知识库…</p>}
        {isError && (
          <button className="nav-item" onClick={() => void refetch()}>
            知识库加载失败，重试
          </button>
        )}
        <nav aria-label="知识库">
          <button className={`nav-item ${!scope.length ? 'active' : ''}`} onClick={() => goScope()}>
            <Icon name="layers" />
            <span>全部知识库</span>
          </button>
          {personalLibs.map((l) => (
            <button
              key={l.id}
              className={`nav-item ${scope.length === 1 && scope[0] === l.id ? 'active' : ''}`}
              onClick={() => goScope(l.id)}
            >
              <Icon name="book" />
              <span>{l.name}</span>
              <span className="count">{l.role}</span>
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
              <span className="count">{l.role}</span>
            </button>
          ))}
        </nav>

        <div className="section-label history-label">
          最近会话
          <Icon name="refresh" />
        </div>
        <nav aria-label="最近会话">
          {history.data?.map((conversation) => (
            <div className="history-row" key={conversation.id}>
              <button
                className={`history-item ${currentId === conversation.id ? 'active' : ''}`}
                title={conversation.title}
                disabled={busy || deletion.isPending}
                onClick={() => {
                  selectConversation(conversation.id!)
                  navigate('/')
                  onClose()
                }}
              >
                {conversation.title}
              </button>
              <button
                className="icon-btn"
                aria-label={`删除会话：${conversation.title}`}
                disabled={busy || deletion.isPending}
                onClick={() =>
                  confirmThen(
                    '删除会话',
                    `确认删除「${conversation.title}」及全部消息？此操作不可恢复。`,
                    '确认删除',
                    () => deletion.mutate(conversation.id!),
                  )
                }
              >
                <Icon name="close" />
              </button>
            </div>
          ))}
          {history.isPending && <p className="table-note">正在加载会话…</p>}
          {history.isError && (
            <button className="history-item" onClick={() => void history.refetch()}>
              会话加载失败，重试
            </button>
          )}
          {history.isSuccess && !history.data.length && <p className="table-note">暂无会话</p>}
        </nav>

        {/* 功能入口由登录返回的菜单树驱动，无权的菜单不渲染。 */}
        {menus.length > 0 && (
          <>
            <div className="section-label">功能菜单</div>
            <nav aria-label="功能菜单">
              {menus.map((m) =>
                m.type === MENU_TYPE_DIR ? (
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
