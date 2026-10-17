import { Button, Card, Form, Input, Typography } from 'antd'
import { LockOutlined, UserOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'

import { useAuthStore } from '@/stores/auth'
import { PRODUCT_NAME } from '@/theme/tokens'

interface LoginForm {
  username: string
  password: string
}

/**
 * 登录页桩：本期无真实鉴权接口，任意非空用户名/密码即写入本地会话，
 * 以便人工验证登录后的三区布局与管理页。
 */
export function LoginPage() {
  const navigate = useNavigate()
  const setSession = useAuthStore((s) => s.setSession)

  const handleFinish = (values: LoginForm) => {
    setSession('dev-token', { id: 1, username: values.username })
    navigate('/', { replace: true })
  }

  return (
    <div className="login-page">
      <Card className="login-card">
        <Typography.Title level={3} className="login-brand">
          {PRODUCT_NAME}
        </Typography.Title>
        <Typography.Paragraph type="secondary">
          本地化知识库平台。当前为骨架版本，登录为占位实现。
        </Typography.Paragraph>

        <Form<LoginForm> layout="vertical" onFinish={handleFinish} requiredMark={false}>
          <Form.Item name="username" label="用户名" rules={[{ required: true, message: '请输入用户名' }]}>
            <Input prefix={<UserOutlined />} placeholder="用户名" autoComplete="username" />
          </Form.Item>
          <Form.Item name="password" label="密码" rules={[{ required: true, message: '请输入密码' }]}>
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="密码"
              autoComplete="current-password"
            />
          </Form.Item>
          <Form.Item style={{ marginBottom: 0 }}>
            <Button type="primary" htmlType="submit" block>
              登录
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  )
}
