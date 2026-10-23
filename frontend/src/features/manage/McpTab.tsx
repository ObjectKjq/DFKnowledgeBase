import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'

import { page, statistics } from '@/api/mcpCallLogController'
import { create1, list1, revoke, updateTools } from '@/api/mcpCredentialController'
import { ModalShell } from '@/components/ModalHost'
import { useAuthStore } from '@/stores/auth'
import { useKb } from '@/stores/knowledge'

const TOOLS = ['search', 'ask', 'list_knowledge_bases']
const time = (value?: string) => value?.replace('T', ' ') ?? '尚未使用'

/** 本人真实 MCP 凭证与脱敏调用记录；完整令牌仅在创建成功后临时显示。 */
export function McpTab() {
  const token = useAuthStore((s) => s.token)
  const admin = useAuthStore((s) => s.roles.includes('instance_admin'))
  const client = useQueryClient()
  const confirm = useKb((s) => s.confirmThen)
  const copy = useKb((s) => s.copyText)
  const [creating, setCreating] = useState(false)
  const [oneTime, setOneTime] = useState<API.McpCredentialCreatedRespVO | null>(null)
  const [pageNum, setPageNum] = useState(1)
  const query = useQuery({
    queryKey: ['mcp-credentials', token],
    queryFn: async () => ({ rows: (await list1()).data ?? [], checkedAt: Date.now() }),
    enabled: !!token,
  })
  const logs = useQuery({
    queryKey: ['mcp-call-logs', token, pageNum],
    queryFn: async () => (await page({ request: { pageNum, pageSize: 20 } })).data,
    enabled: !!token,
  })
  const stats = useQuery({
    queryKey: ['mcp-statistics', token],
    queryFn: async () => (await statistics()).data ?? [],
    enabled: !!token && admin,
  })
  const refresh = () => client.invalidateQueries({ queryKey: ['mcp-credentials'] })
  const create = useMutation({
    mutationFn: create1,
    onSuccess: (response) => {
      setCreating(false)
      setOneTime(response.data ?? null)
      void refresh()
    },
  })
  const revocation = useMutation({
    mutationFn: (id: number) => revoke({ id }),
    onSuccess: () => {
      void refresh()
    },
  })
  const tools = useMutation({
    mutationFn: updateTools,
    onSuccess: () => {
      void refresh()
    },
  })
  const endpoint = `${window.location.origin}/api/mcp`
  const config = JSON.stringify(
    {
      mcpServers: {
        knowledge: { type: 'http', url: endpoint, headers: { Authorization: 'Bearer YOUR_TOKEN' } },
      },
    },
    null,
    2,
  )
  const clearToken = () => {
    setOneTime(null)
    create.reset()
  }
  return (
    <>
      <div className="info-banner">
        凭证仅允许三个只读工具，访问范围跟随你当前权限。默认 90 天到期，可设 1～365
        天；撤销后即时失效。
      </div>
      <div className="manage-toolbar">
        <h2>我的 MCP 凭证</h2>
        <button className="btn primary" onClick={() => setCreating(true)}>
          新建凭证
        </button>
      </div>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>名称</th>
              <th>允许工具</th>
              <th>状态</th>
              <th>到期时间</th>
              <th>最后使用</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {query.data?.rows.map((credential) => {
              const expired =
                !credential.expiresAt ||
                new Date(credential.expiresAt).getTime() <= query.data.checkedAt
              const active = !credential.revokedAt && !expired
              return (
                <tr key={credential.id}>
                  <td>
                    {credential.name}
                    <small>{credential.tokenPrefix}</small>
                  </td>
                  <td>
                    {TOOLS.map((tool) => (
                      <label className="menu-option" key={tool}>
                        <input
                          type="checkbox"
                          aria-label={`${credential.name}允许${tool}`}
                          checked={credential.tools?.includes(tool) ?? false}
                          disabled={!active || tools.isPending}
                          onChange={(e) =>
                            tools.mutate({
                              id: credential.id!,
                              tools: e.target.checked
                                ? [...(credential.tools ?? []), tool]
                                : (credential.tools ?? []).filter((value) => value !== tool),
                            })
                          }
                        />
                        {tool}
                      </label>
                    ))}
                  </td>
                  <td>{credential.revokedAt ? '已撤销' : expired ? '已过期' : '有效'}</td>
                  <td>{time(credential.expiresAt)}</td>
                  <td>{time(credential.lastUsedAt)}</td>
                  <td>
                    {active && (
                      <button
                        disabled={revocation.isPending}
                        onClick={() =>
                          confirm(
                            '撤销凭证',
                            `撤销「${credential.name}」后新调用立即被拒绝。`,
                            '确认撤销',
                            () => revocation.mutate(credential.id!),
                          )
                        }
                      >
                        撤销
                      </button>
                    )}
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
      {query.isPending && <p role="status">正在加载凭证…</p>}
      {query.isSuccess && !query.data.rows.length && <p>还没有凭证，创建一份开始连接。</p>}
      {query.isError && <button onClick={() => void query.refetch()}>凭证加载失败，重试</button>}
      <section className="settings-card">
        <h3>接入 AI 智能体</h3>
        <p>端点：{endpoint}</p>
        <p>
          使用支持自定义 Bearer 请求头的 Streamable HTTP 客户端。将 YOUR_TOKEN
          替换为创建时展示的完整令牌。协议版本 2025-11-25，不提供 OAuth 自动授权。
        </p>
        <pre className="code-box">{config}</pre>
        <button className="btn ghost" onClick={() => void copy(config)}>
          复制接入配置
        </button>
        <p>
          后续请求头 MCP-Protocol-Version: 2025-11-25；Accept: application/json, text/event-stream。
        </p>
      </section>
      <section className="settings-card">
        <h3>我的调用记录</h3>
        <p>包含成功、失败和拒绝调用，不包含文档正文及令牌明文。</p>
        <button className="btn ghost" onClick={() => void logs.refetch()}>
          刷新调用记录
        </button>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>时间</th>
                <th>工具</th>
                <th>目标知识库</th>
                <th>结果条数</th>
                <th>凭证</th>
                <th>结果与原因</th>
              </tr>
            </thead>
            <tbody>
              {logs.data?.list?.map((log) => (
                <tr key={log.id}>
                  <td>{time(log.callTime)}</td>
                  <td>{log.toolName}</td>
                  <td>{log.knowledgeBaseIds ?? '—'}</td>
                  <td>{log.resultCount ?? 0}</td>
                  <td>
                    {query.data?.rows.find((credential) => credential.id === log.credentialId)
                      ?.name ?? log.credentialId}
                  </td>
                  <td>
                    {(
                      { SUCCESS: '成功', FAILED: '失败', REJECTED: '拒绝' } as Record<
                        string,
                        string
                      >
                    )[log.outcome ?? ''] ?? log.outcome}{' '}
                    {log.reasonCode}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {logs.isPending && <p role="status">正在加载调用记录…</p>}
        {logs.isSuccess && !logs.data?.list?.length && <p>暂无调用记录</p>}
        {logs.isError && (
          <button onClick={() => void logs.refetch()}>调用记录加载失败，重试</button>
        )}
        <div className="pager">
          <span>共 {logs.data?.total ?? 0} 条</span>
          <button
            disabled={pageNum <= 1 || logs.isFetching}
            onClick={() => setPageNum(pageNum - 1)}
          >
            上一页
          </button>
          <span>{pageNum}</span>
          <button
            disabled={pageNum * 20 >= (logs.data?.total ?? 0) || logs.isFetching}
            onClick={() => setPageNum(pageNum + 1)}
          >
            下一页
          </button>
        </div>
      </section>
      {admin && (
        <section className="settings-card">
          <h3>实例调用统计</h3>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>工具</th>
                  <th>调用结果</th>
                  <th>次数</th>
                </tr>
              </thead>
              <tbody>
                {stats.data?.map((item, index) => (
                  <tr key={index}>
                    <td>{String(item.toolName ?? item.toolname ?? '—')}</td>
                    <td>
                      {(
                        { SUCCESS: '成功', FAILED: '失败', REJECTED: '拒绝' } as Record<
                          string,
                          string
                        >
                      )[String(item.outcome)] ?? String(item.outcome ?? '—')}
                    </td>
                    <td>{String(item.callCount ?? item.callcount ?? 0)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {stats.isError && (
            <button onClick={() => void stats.refetch()}>统计加载失败，重试</button>
          )}
        </section>
      )}
      {creating && (
        <CredentialForm
          busy={create.isPending}
          onClose={() => {
            if (!create.isPending) setCreating(false)
          }}
          onCreate={(body) => create.mutate(body)}
        />
      )}
      {oneTime && (
        <ModalShell
          title="保存一次性令牌"
          description="完整令牌仅展示这一次，关闭后无法重新读取。"
          onClose={clearToken}
        >
          <pre className="code-box">{oneTime.token}</pre>
          <p>{oneTime.instructions}</p>
          <div className="form-actions">
            <button className="btn ghost" onClick={() => void copy(oneTime.token ?? '')}>
              复制令牌
            </button>
            <button className="btn primary" onClick={clearToken}>
              已妥善保存
            </button>
          </div>
        </ModalShell>
      )}
    </>
  )
}

function CredentialForm({
  busy,
  onClose,
  onCreate,
}: {
  busy: boolean
  onClose: () => void
  onCreate: (body: API.McpCredentialCreateReqVO) => void
}) {
  const [name, setName] = useState('')
  const [validDays, setValidDays] = useState(90)
  const [tools, setTools] = useState([...TOOLS])
  return (
    <ModalShell
      title="新建 MCP 凭证"
      description="凭证仅访问你当前有权的知识库，工具可独立开关。"
      onClose={onClose}
    >
      <form
        onSubmit={(event) => {
          event.preventDefault()
          onCreate({ name: name.trim(), validDays, tools })
        }}
      >
        <label className="field">
          凭证名称
          <input required maxLength={80} value={name} onChange={(e) => setName(e.target.value)} />
        </label>
        <label className="field">
          有效天数
          <input
            type="number"
            required
            min={1}
            max={365}
            value={validDays}
            onChange={(e) => setValidDays(Number(e.target.value))}
          />
        </label>
        {TOOLS.map((tool) => (
          <label className="menu-option" key={tool}>
            <input
              type="checkbox"
              checked={tools.includes(tool)}
              onChange={(e) =>
                setTools(
                  e.target.checked ? [...tools, tool] : tools.filter((value) => value !== tool),
                )
              }
            />
            {tool}
          </label>
        ))}
        <div className="form-actions">
          <button type="button" className="btn ghost" disabled={busy} onClick={onClose}>
            取消
          </button>
          <button className="btn primary" disabled={busy || !name.trim()}>
            创建凭证
          </button>
        </div>
      </form>
    </ModalShell>
  )
}
