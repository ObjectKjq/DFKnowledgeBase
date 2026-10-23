import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'

import { create, deleteUsingDelete, list, test, update1 } from '@/api/modelConfigurationController'
import { useAuthStore } from '@/stores/auth'
import { useKb } from '@/stores/knowledge'
import { InstanceSettings } from './InstanceSettings'

/** 实例管理员维护真实命名模型；普通用户联系管理员。 */
export function ModelsTab() {
  const admin = useAuthStore((s) => s.roles.includes('instance_admin'))
  if (!admin) return <p>模型接入与实例运行设置仅限实例管理员维护，请联系管理员。</p>
  return (
    <>
      <ModelManagement />
      <InstanceSettings />
    </>
  )
}

function ModelManagement() {
  const token = useAuthStore((s) => s.token)
  const client = useQueryClient()
  const confirm = useKb((s) => s.confirmThen)
  const [editing, setEditing] = useState<API.ModelConfigurationRespVO | null>(null)
  const query = useQuery({
    queryKey: ['manage-models', token],
    queryFn: async () => (await list()).data ?? [],
  })
  const refresh = () => client.invalidateQueries({ queryKey: ['manage-models'] })
  const check = useMutation({
    mutationFn: (id: number) => test({ id }),
    onSuccess: () => {
      void refresh()
    },
  })
  const remove = useMutation({
    mutationFn: (id: number) => deleteUsingDelete({ id }),
    onSuccess: () => {
      void refresh()
    },
  })
  return (
    <>
      <div className="info-banner">
        文本向量固定使用内置 bge-small-zh-v15（512
        维）。重排模型可选，未配置时使用融合排序；模型故障会明确报告。
      </div>
      <div className="manage-toolbar">
        <h2>命名模型配置</h2>
        <button
          className="btn primary"
          onClick={() => setEditing({ kind: 'CHAT', timeoutSeconds: 60 })}
        >
          新增模型
        </button>
      </div>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>配置名称</th>
              <th>用途</th>
              <th>模型</th>
              <th>连接状态</th>
              <th>最近测试</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {(query.data ?? []).map((model) => (
              <tr key={model.id}>
                <td>
                  {model.name}
                  {model.simulated && <span className="tag oat">本地模拟 · 非真实生成</span>}
                </td>
                <td>{model.kind === 'CHAT' ? '对话模型' : '重排模型'}</td>
                <td>{model.model}</td>
                <td>
                  {model.testStatus ?? '未测试'}
                  {model.testError && <p role="alert">{model.testError}</p>}
                </td>
                <td>{model.lastTestTime?.replace('T', ' ') ?? '—'}</td>
                <td>
                  <span className="table-actions">
                    <button disabled={check.isPending} onClick={() => check.mutate(model.id!)}>
                      测试连接
                    </button>
                    <button onClick={() => setEditing(model)}>编辑</button>
                    <button
                      disabled={remove.isPending}
                      onClick={() =>
                        confirm(
                          '删除模型',
                          `确认删除「${model.name}」？使用此配置的后续请求将无法执行。`,
                          '确认删除',
                          () => remove.mutate(model.id!),
                        )
                      }
                    >
                      删除
                    </button>
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {query.isPending && <p role="status">正在加载模型…</p>}
      {query.isError && <button onClick={() => void query.refetch()}>模型加载失败，重试</button>}
      {query.isSuccess && !query.data.length && <p>尚未配置模型，请新增命名对话模型。</p>}
      {editing && (
        <ModelForm
          key={editing.id ?? 'new'}
          model={editing}
          onClose={() => setEditing(null)}
          onSaved={() => {
            setEditing(null)
            void refresh()
            void client.invalidateQueries({ queryKey: ['chat-models'] })
          }}
        />
      )}
    </>
  )
}

function ModelForm({
  model,
  onClose,
  onSaved,
}: {
  model: API.ModelConfigurationRespVO
  onClose: () => void
  onSaved: () => void
}) {
  const [form, setForm] = useState<API.ModelConfigurationSaveReqVO>({
    id: model.id,
    name: model.name ?? '',
    kind: model.kind ?? 'CHAT',
    baseUrl: model.baseUrl ?? '',
    model: model.model ?? '',
    timeoutSeconds: model.timeoutSeconds ?? 60,
  })
  const [key, setKey] = useState('')
  const [clearKey, setClearKey] = useState(false)
  const save = useMutation({
    mutationFn: async () => {
      const body = { ...form, apiKey: clearKey ? '' : key || undefined }
      if (form.id) await update1(body)
      else await create(body)
    },
    onSuccess: onSaved,
  })
  return (
    <form
      className="settings-card"
      aria-label="模型配置"
      onSubmit={(event) => {
        event.preventDefault()
        save.mutate()
      }}
    >
      <h3>{form.id ? '编辑模型' : '新增模型'}</h3>
      <div className="form-grid">
        <label className="field">
          配置名称
          <input
            required
            maxLength={100}
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
          />
        </label>
        <label className="field">
          用途
          <select value={form.kind} onChange={(e) => setForm({ ...form, kind: e.target.value })}>
            <option value="CHAT">对话模型</option>
            <option value="RERANK">重排模型</option>
          </select>
        </label>
        <label className="field">
          服务地址
          <input
            required
            value={form.baseUrl}
            onChange={(e) => setForm({ ...form, baseUrl: e.target.value })}
          />
        </label>
        <label className="field">
          提供方模型名
          <input
            required
            value={form.model}
            onChange={(e) => setForm({ ...form, model: e.target.value })}
          />
        </label>
        <label className="field">
          API 密钥
          <input
            type="password"
            autoComplete="new-password"
            value={key}
            disabled={clearKey}
            placeholder={model.hasApiKey ? '已保存密钥，留空保留' : '按需填写'}
            onChange={(e) => setKey(e.target.value)}
          />
        </label>
        {model.hasApiKey && (
          <label>
            <input
              type="checkbox"
              checked={clearKey}
              onChange={(e) => setClearKey(e.target.checked)}
            />
            清除已保存密钥
          </label>
        )}
        <label className="field">
          超时秒数
          <input
            type="number"
            min={1}
            max={300}
            required
            value={form.timeoutSeconds}
            onChange={(e) => setForm({ ...form, timeoutSeconds: Number(e.target.value) })}
          />
        </label>
      </div>
      <div className="form-actions">
        <button type="button" className="btn ghost" disabled={save.isPending} onClick={onClose}>
          取消
        </button>
        <button className="btn primary" disabled={save.isPending}>
          保存模型
        </button>
      </div>
    </form>
  )
}
