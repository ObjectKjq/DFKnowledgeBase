import { useEffect, type ReactElement } from 'react'
import { Navigate, useNavigate, useParams } from 'react-router-dom'

import { TAB_KEYS, TAB_TITLES } from '@/mock/data'
import { useKb } from '@/stores/knowledge'

import { Icon } from '@/components/Icon'

import { AccountTab } from './AccountTab'
import { DocsTab } from './DocsTab'
import { McpTab } from './McpTab'
import { MembersTab } from './MembersTab'
import { ModelsTab } from './ModelsTab'

const TABS: Record<string, () => ReactElement> = {
  docs: DocsTab,
  mcp: McpTab,
  models: ModelsTab,
  members: MembersTab,
  account: AccountTab,
}

/** 管理页（复刻原型 `#management` / renderManage）。五个 Tab 由子路由承载。 */
export function ManagementPage() {
  const { tab } = useParams()
  const navigate = useNavigate()
  const closePreview = useKb((s) => s.closePreview)
  const closePopover = useKb((s) => s.closePopover)

  const active = tab && TAB_KEYS.includes(tab) ? tab : 'docs'

  // 原型 showManage：进入管理页时关闭预览抽屉与输入选项弹层。
  useEffect(() => {
    closePreview()
    closePopover()
  }, [closePreview, closePopover])

  if (tab && !TAB_KEYS.includes(tab)) {
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
          {TAB_KEYS.map((k) => (
            <button
              key={k}
              className={active === k ? 'active' : ''}
              onClick={() => navigate(`/settings/${k}`)}
            >
              {TAB_TITLES[k]}
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
