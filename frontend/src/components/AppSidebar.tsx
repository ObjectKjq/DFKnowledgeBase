import { Button, Divider, Menu, Typography } from 'antd'
import { CommentOutlined, LogoutOutlined, PlusOutlined, SettingOutlined } from '@ant-design/icons'
import { useLocation, useNavigate } from 'react-router-dom'

import { useAuthStore } from '@/stores/auth'
import { PRODUCT_NAME } from '@/theme/tokens'

const MENU_STYLE = { background: 'transparent' } as const

export function AppSidebar() {
  const navigate = useNavigate()
  const location = useLocation()
  const user = useAuthStore((s) => s.user)
  const clearSession = useAuthStore((s) => s.clearSession)

  const inChat = !location.pathname.startsWith('/settings')

  return (
    <aside className="app-sider">
      <div className="app-sider-brand">{PRODUCT_NAME}</div>

      <Button type="primary" block icon={<PlusOutlined />} onClick={() => navigate('/')}>
        新检索
      </Button>

      <section className="app-sider-section">
        <div className="app-sider-title">知识库</div>
        <Menu
          mode="inline"
          style={MENU_STYLE}
          selectedKeys={inChat ? ['/'] : []}
          items={[{ key: '/', icon: <CommentOutlined />, label: '我的知识库' }]}
          onClick={({ key }) => navigate(key)}
        />
      </section>

      <section className="app-sider-section">
        <div className="app-sider-title">最近会话</div>
        <Typography.Text type="secondary">暂无会话</Typography.Text>
      </section>

      <div className="app-sider-footer">
        <Button
          type="text"
          block
          icon={<SettingOutlined />}
          onClick={() => navigate('/settings')}
        >
          设置
        </Button>
        <Divider style={{ margin: '8px 0' }} />
        <div className="app-sider-user">
          <Typography.Text ellipsis>{user?.username ?? '未登录'}</Typography.Text>
          <Button
            type="text"
            size="small"
            title="退出登录"
            icon={<LogoutOutlined />}
            onClick={() => {
              clearSession()
              navigate('/login')
            }}
          />
        </div>
      </div>
    </aside>
  )
}
