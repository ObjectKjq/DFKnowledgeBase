import { Typography } from 'antd'

/** 各管理子页的统一占位面板：本轮只交付可运行骨架，不还原具体业务界面。 */
export function PlaceholderPanel({ title, description }: { title: string; description: string }) {
  return (
    <div className="placeholder-panel">
      <Typography.Title level={4}>{title}</Typography.Title>
      <Typography.Paragraph type="secondary">{description}</Typography.Paragraph>
    </div>
  )
}
