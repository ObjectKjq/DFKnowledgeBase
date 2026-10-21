import { useLocation, useNavigate } from 'react-router-dom'

import { TAB_CONFIGS } from '@/features/manage/tabs'
import { useAuthStore } from '@/stores/auth'
import { useConversation } from '@/features/chat/conversations'

import { Icon } from './Icon'

/** 顶部面包屑与账号区（复刻原型 `.topbar`）。 */
export function AppTopbar({ onToggleSidebar }: { onToggleSidebar: () => void }) {
  const location = useLocation()
  const navigate = useNavigate()
  const { data } = useConversation()
  const title = data?.title ?? '新检索'
  const user = useAuthStore((s) => s.user)

  const isSettings = location.pathname.startsWith('/settings')
  const tab = location.pathname.split('/')[2] ?? 'docs'
  const currentLabel = isSettings
    ? (TAB_CONFIGS.find((t) => t.key === tab)?.title ?? '设置')
    : title === '新检索'
      ? '新检索'
      : title.slice(0, 18)
  const avatar = (user?.username ?? '林').slice(0, 1)

  return (
    <header className="topbar">
      <div className="breadcrumb">
        <button className="icon-btn mobile-menu" onClick={onToggleSidebar} aria-label="打开导航">
          <Icon name="menu" />
        </button>
        <Icon name="layers" />
        <span>工作空间</span>
        <span className="slash">/</span>
        <span>{currentLabel}</span>
      </div>
      <div className="top-right">
        <span className="local-badge" title="检索和问答基于当前用户有权访问的知识库。">
          <span className="dot" />
          本地知识
        </span>
        <button
          className="avatar"
          aria-label="账号设置"
          onClick={() => navigate('/settings/account')}
        >
          {avatar}
        </button>
      </div>
    </header>
  )
}
