import { useQuery } from '@tanstack/react-query'
import { Dropdown } from 'antd'
import { useState } from 'react'

import { simpleList } from '@/api/modelConfigurationController'
import { Icon } from '@/components/Icon'
import { useAuthStore } from '@/stores/auth'

/** 输入区的命名对话模型选择，沿用会话选择并标识本地模拟模型。 */
export function ModelPicker({
  value,
  onChange,
}: {
  value?: number
  onChange: (value?: number) => void
}) {
  const token = useAuthStore((s) => s.token)
  const [open, setOpen] = useState(false)
  const query = useQuery({
    queryKey: ['chat-models', token],
    queryFn: async () => (await simpleList()).data ?? [],
    enabled: !!token,
  })
  const models = (query.data ?? []).filter((model) => model.id !== undefined)
  const selected = models.find((model) => model.id === value)
  const label = query.isPending
    ? '加载模型…'
    : query.isError
      ? '模型加载失败'
      : !models.length
        ? '未配置对话模型'
        : (selected?.name ?? (value === undefined ? '默认模型' : '模型已不可用'))

  return (
    <>
      <Dropdown
        trigger={['click']}
        placement="topRight"
        open={open}
        onOpenChange={setOpen}
        disabled={!token || query.isPending || query.isError || !models.length}
        destroyOnHidden
        classNames={{ root: 'model-picker-dropdown' }}
        popupRender={(menu) => (
          <div className="model-menu-surface">
            <div className="model-menu-heading">选择对话模型</div>
            {menu}
          </div>
        )}
        menu={{
          selectable: true,
          selectedKeys: [value === undefined ? 'default' : String(value)],
          items: [
            { key: 'default', label: '默认模型' },
            ...models.map((model) => ({
              key: String(model.id),
              label: (
                <span className="model-menu-option">
                  <span className="model-menu-name">{model.name}</span>
                  {model.simulated && <span className="model-simulated-badge">本地模拟</span>}
                  <span className="model-option-check">
                    {model.id === value && <Icon name="check" />}
                  </span>
                </span>
              ),
            })),
          ],
          onClick: ({ key }) => {
            onChange(key === 'default' ? undefined : Number(key))
            setOpen(false)
          },
        }}
      >
        <button
          type="button"
          className="model-picker-trigger"
          aria-label="对话模型"
          aria-haspopup="menu"
          aria-expanded={open}
          disabled={!token || query.isPending || query.isError || !models.length}
        >
          <Icon name="spark" />
          <span className="model-trigger-label">{label}</span>
          {selected?.simulated && <span className="model-simulated-badge">本地模拟</span>}
          <Icon name="down" />
        </button>
      </Dropdown>
      {query.isError && (
        <button type="button" className="text-btn" onClick={() => void query.refetch()}>
          重试加载模型
        </button>
      )}
    </>
  )
}
