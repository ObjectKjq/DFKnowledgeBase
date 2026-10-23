import { useMutation, useQuery } from '@tanstack/react-query'
import { useState } from 'react'

import { get, update } from '@/api/instanceSettingsController'
import { useAuthStore } from '@/stores/auth'

/** 实例运行设置从服务端读取，调整后立即参与入库调度及 MCP 调用。 */
export function InstanceSettings() {
  const token = useAuthStore((s) => s.token)
  const query = useQuery({
    queryKey: ['instance-settings', token],
    queryFn: async () => (await get()).data,
  })
  if (query.isPending) return <p role="status">正在加载运行设置…</p>
  if (query.isError || !query.data)
    return <button onClick={() => void query.refetch()}>运行设置加载失败，重试</button>
  return <SettingsForm settings={query.data as API.InstanceSettingsSaveReqVO} />
}

function SettingsForm({ settings }: { settings: API.InstanceSettingsSaveReqVO }) {
  const [form, setForm] = useState(settings)
  const [saved, setSaved] = useState(false)
  const save = useMutation({ mutationFn: () => update(form), onSuccess: () => setSaved(true) })
  const change = (values: Partial<API.InstanceSettingsSaveReqVO>) => {
    setForm({ ...form, ...values })
    setSaved(false)
  }
  return (
    <form
      className="settings-card"
      onSubmit={(event) => {
        event.preventDefault()
        save.mutate()
      }}
    >
      <h3>实例运行设置</h3>
      <label>
        <input
          type="checkbox"
          checked={form.mcpEnabled}
          onChange={(e) => change({ mcpEnabled: e.target.checked })}
        />
        开启 MCP 接入
      </label>
      <div className="form-grid">
        <label className="field">
          每凭证每分钟调用上限
          <input
            required
            type="number"
            min={1}
            max={100000}
            value={form.mcpRateLimitPerMinute}
            onChange={(e) => change({ mcpRateLimitPerMinute: Number(e.target.value) })}
          />
        </label>
        <label className="field">
          全实例入库并发
          <input
            required
            type="number"
            min={1}
            max={64}
            value={form.ingestGlobalConcurrency}
            onChange={(e) => change({ ingestGlobalConcurrency: Number(e.target.value) })}
          />
        </label>
        <label className="field">
          单知识库入库并发
          <input
            required
            type="number"
            min={1}
            max={64}
            value={form.ingestKnowledgeBaseConcurrency}
            onChange={(e) => change({ ingestKnowledgeBaseConcurrency: Number(e.target.value) })}
          />
        </label>
      </div>
      {saved && <p role="status">运行设置已保存</p>}
      <div className="form-actions">
        <button className="btn primary" disabled={save.isPending}>
          保存运行设置
        </button>
      </div>
    </form>
  )
}
