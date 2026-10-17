import { SendOutlined, ApiOutlined, PictureOutlined, UploadOutlined } from '@ant-design/icons'
import { Button, Input, Select, Space, Tag, Typography } from 'antd'
import { useState } from 'react'

/** 检索模式（PRD §6.3 的权威定义，本期只做选择器占位）。 */
const SEARCH_MODES = [
  { value: 'direct', label: '直接检索' },
  { value: 'exact', label: '精确检索' },
  { value: 'fuzzy', label: '模糊检索' },
  { value: 'guided', label: '引导检索' },
  { value: 'rag', label: 'RAG 检索' },
]

const MODELS = [
  { value: 'default', label: '默认对话模型' },
  { value: 'standard', label: '标准档' },
  { value: 'quality', label: '高质量档' },
]

/** 首屏空态的三个快捷入口（PRD §6.1）。 */
const QUICK_ENTRIES = [
  { key: 'upload', icon: <UploadOutlined />, label: '上传资料' },
  { key: 'image', icon: <PictureOutlined />, label: '试试图片搜索' },
  { key: 'mcp', icon: <ApiOutlined />, label: '连接 AI 智能体' },
]

export function ChatPage() {
  const [text, setText] = useState('')

  return (
    <div className="chat-page">
      <div className="chat-stream">
        <div className="chat-greeting">
          <Typography.Title level={2} className="chat-greeting-title">
            今天想找什么？
          </Typography.Title>
          <Space wrap>
            {QUICK_ENTRIES.map((entry) => (
              <Button key={entry.key} icon={entry.icon}>
                {entry.label}
              </Button>
            ))}
          </Space>
        </div>
      </div>

      <div className="chat-composer">
        <Space size={6} wrap>
          <Tag.CheckableTag checked>全局检索</Tag.CheckableTag>
          <Tag.CheckableTag checked>联网搜索</Tag.CheckableTag>
          <Tag.CheckableTag checked>图片搜索</Tag.CheckableTag>
        </Space>

        <Input.TextArea
          value={text}
          onChange={(event) => setText(event.target.value)}
          placeholder="输入问题，或把图片拖进来…"
          autoSize={{ minRows: 2, maxRows: 6 }}
        />

        <div className="chat-composer-footer">
          <Button type="text" size="small" icon={<UploadOutlined />}>
            上传附件
          </Button>
          <Space size={8}>
            <Select size="small" defaultValue="direct" options={SEARCH_MODES} style={{ width: 108 }} />
            <Select size="small" defaultValue="default" options={MODELS} style={{ width: 148 }} />
            <Button type="primary" icon={<SendOutlined />} />
          </Space>
        </div>
      </div>
    </div>
  )
}
