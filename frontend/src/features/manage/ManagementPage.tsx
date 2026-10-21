import { useEffect, type ReactElement } from 'react'
import { Navigate, useNavigate, useParams } from 'react-router-dom'

import { hasMenuPath, useAuthStore } from '@/stores/auth'
import { useChat } from '@/stores/chat'
import { useKb } from '@/stores/knowledge'

import { Icon } from '@/components/Icon'

import { AccountTab } from './AccountTab'
import { DocsTab } from './DocsTab'
import { LibrariesTab } from './LibrariesTab'
import { McpTab } from './McpTab'
import { MenusTab } from './MenusTab'
import { ModelsTab } from './ModelsTab'
import { RolesTab } from './RolesTab'
import { TAB_CONFIGS } from './tabs'
import { UsersTab } from './UsersTab'

const TABS: Record<string, () => ReactElement> = {
  docs: DocsTab,
  libraries: LibrariesTab,
  mcp: McpTab,
  models: ModelsTab,
  users: UsersTab,
  roles: RolesTab,
  menus: MenusTab,
  account: AccountTab,
}

/** 管理页（复刻原型 `#management` / renderManage）。页签由子路由承载，按菜单树显隐。 */
export function ManagementPage() {
  const { tab } = useParams()
  const navigate = useNavigate()
  const closePreview = useChat((s) => s.closePreview)
  const closePopover = useKb((s) => s.closePopover)
  const menus = useAuthStore((s) => s.menus)

  const tabs = TAB_CONFIGS.filter((t) => hasMenuPath(menus, t.menuPath))
  const active = tab && tabs.some((t) => t.key === tab) ? tab : (tabs[0]?.key ?? 'docs')

  // 原型 showManage：进入管理页时关闭预览抽屉与输入选项弹层。
  useEffect(() => {
    closePreview()
    closePopover()
  }, [closePreview, closePopover])

  if (tab && !tabs.some((t) => t.key === tab)) {
    return <Navigate to="/settings/docs" replace />
  }

  const Body = TABS[active]

  return (
    <div className="management" id="management">
      <div className="manage-inner">
        <button className="back-chat" onClick={() => navigate('/')}>
          <Icon name="back" />
          回到检索
        </button>
        <div className="page-kicker">YOUR KNOWLEDGE, WELL KEPT.</div>
        <h1 className="page-title">让知识，有序生长。</h1>
        <p className="page-subtitle">整理资料，连接工具，照顾好属于你的知识空间。</p>
        <nav className="manage-tabs" aria-label="管理模块">
          {tabs.map((t) => (
            <button
              key={t.key}
              className={active === t.key ? 'active' : ''}
              onClick={() => navigate(`/settings/${t.key}`)}
            >
              {t.title}
            </button>
          ))}
        </nav>
        <div id="manage-body">
          <Body />
        </div>
      </div>
    </div>
  )
}
