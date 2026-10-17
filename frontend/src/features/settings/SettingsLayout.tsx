import {
  ApiOutlined,
  ArrowLeftOutlined,
  DatabaseOutlined,
  FileTextOutlined,
  SettingOutlined,
  UserOutlined,
} from '@ant-design/icons'
import { Button, Menu, Typography } from 'antd'
import { Outlet, useLocation, useNavigate } from 'react-router-dom'

import { PRODUCT_NAME } from '@/theme/tokens'

const MENU_ITEMS = [
  { key: '/settings/knowledge-bases', icon: <DatabaseOutlined />, label: '知识库管理' },
  { key: '/settings/documents', icon: <FileTextOutlined />, label: '文档管理' },
  { key: '/settings/mcp', icon: <ApiOutlined />, label: 'MCP 凭证' },
  { key: '/settings/instance', icon: <SettingOutlined />, label: '实例设置' },
  { key: '/settings/account', icon: <UserOutlined />, label: '账号' },
]

/** 管理页外壳：左侧子菜单 + 右侧子页（PRD §6.4）。 */
export function SettingsLayout() {
  const navigate = useNavigate()
  const location = useLocation()

  return (
    <div className="settings-shell">
      <aside className="settings-sider">
        <Button
          type="text"
          size="small"
          icon={<ArrowLeftOutlined />}
          onClick={() => navigate('/')}
        >
          返回会话
        </Button>
        <Typography.Title level={4} className="settings-title">
          {PRODUCT_NAME} · 设置
        </Typography.Title>
        <Menu
          mode="inline"
          style={{ background: 'transparent' }}
          selectedKeys={[location.pathname]}
          items={MENU_ITEMS}
          onClick={({ key }) => navigate(key)}
        />
      </aside>

      <div className="settings-main">
        <Outlet />
      </div>
    </div>
  )
}
