import { useLocation, useNavigate } from 'react-router-dom'

import { TAB_CONFIGS } from '@/features/manage/tabs'
import { useAuthStore } from '@/stores/auth'
import { useConversation } from '@/features/chat/conversations'

import { Icon } from './Icon'

/** 轻量工作区标题栏，提供桌面侧栏折叠与移动导航入口。 */
export function AppTopbar({
  onToggleSidebar,
  onToggleDesktopSidebar,
  sidebarCollapsed,
  sidebarOpen,
}: {
  onToggleSidebar: () => void
  onToggleDesktopSidebar: () => void
  sidebarCollapsed: boolean
  sidebarOpen: boolean
}) {
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
        <button
          className="icon-btn desktop-sidebar-toggle"
          onClick={onToggleDesktopSidebar}
          aria-label={sidebarCollapsed ? '展开导航' : '收起导航'}
          aria-expanded={!sidebarCollapsed}
          aria-controls="app-sidebar"
          title={sidebarCollapsed ? '展开导航' : '收起导航'}
        >
          <Icon name="menu" />
        </button>
        <button
          className="icon-btn mobile-menu"
          onClick={onToggleSidebar}
          aria-label="打开导航"
          aria-expanded={sidebarOpen}
          aria-controls="app-sidebar"
        >
          <Icon name="menu" />
        </button>
        <span className="workspace-label">{isSettings ? '设置与管理' : '知识空间'}</span>
        <span className="slash">/</span>
        <span className="current-page" title={isSettings ? currentLabel : title}>
          {currentLabel}
        </span>
      </div>
      <div className="top-right">
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
