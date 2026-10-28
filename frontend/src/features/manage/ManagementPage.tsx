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

/** 分组设置导航与内容工作区，入口仍按服务端菜单树显隐。 */
export function ManagementPage() {
  const { tab } = useParams()
  const navigate = useNavigate()
  const closePreview = useChat((s) => s.closePreview)
  const closePopover = useKb((s) => s.closePopover)
  const menus = useAuthStore((s) => s.menus)
  const admin = useAuthStore((s) => s.roles.includes('instance_admin'))

  const tabs = TAB_CONFIGS.filter(
    (t) => (t.key !== 'models' || admin) && hasMenuPath(menus, t.menuPath),
  )
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
  const current = tabs.find((t) => t.key === active)!

  return (
    <div className="management" id="management">
      <div className="manage-inner">
        <nav className="manage-navigation" aria-label="管理模块">
          <div className="manage-nav-heading">设置与管理</div>
          {(['知识空间', '实例管理', '个人'] as const).map((group) => {
            const items = tabs.filter((t) => t.group === group)
            if (!items.length) return null
            return (
              <div className="manage-nav-group" key={group}>
                <div className="manage-nav-label">{group}</div>
                {items.map((t) => (
                  <button
                    key={t.key}
                    className={active === t.key ? 'active' : ''}
                    aria-current={active === t.key ? 'page' : undefined}
                    onClick={() => navigate(`/settings/${t.key}`)}
                  >
                    <Icon name={t.icon} />
                    {t.title}
                  </button>
                ))}
              </div>
            )
          })}
          <button className="back-chat" onClick={() => navigate('/')}>
            <Icon name="back" />
            回到检索
          </button>
        </nav>
        <div className="manage-content">
          <header className="manage-page-header">
            <h1 className="page-title">{current.title}</h1>
            <p className="page-subtitle">{current.description}</p>
          </header>
          <div id="manage-body" key={active}>
            <Body />
          </div>
        </div>
      </div>
    </div>
  )
}
