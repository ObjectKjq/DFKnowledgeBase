import { useState } from 'react'
import { Outlet } from 'react-router-dom'
import { Button, Drawer, Space, Typography } from 'antd'

import { useAuthStore } from '@/stores/auth'

import { AppSidebar } from './AppSidebar'

/** 三区布局壳：左侧栏 + 主区 + 右侧预览抽屉（抽屉由 antd Drawer 承载）。 */
export function AppLayout() {
  const user = useAuthStore((s) => s.user)
  const [preview, setPreview] = useState({ open: false, title: '' })

  return (
    <div className="app-shell">
      <AppSidebar />

      <div className="app-main">
        <header className="app-topbar">
          <Typography.Text strong>新检索</Typography.Text>
          <Space size={12}>
            {/* 占位触发：后续右侧抽屉由结果卡片/引用点击打开 */}
            <Button
              type="text"
              size="small"
              onClick={() => setPreview({ open: true, title: '示例 · 命中片段预览' })}
            >
              预览抽屉
            </Button>
            <Typography.Text type="secondary">{user?.username ?? '未登录'}</Typography.Text>
          </Space>
        </header>

        <div className="app-content">
          <Outlet />
        </div>
      </div>

      <Drawer
        open={preview.open}
        title={preview.title}
        placement="right"
        width={420}
        onClose={() => setPreview((prev) => ({ ...prev, open: false }))}
      >
        <Typography.Paragraph type="secondary">
          原文预览占位。后续在此展示命中分块或图片资源，并自动滚动定位到命中处。
        </Typography.Paragraph>
      </Drawer>
    </div>
  )
}
