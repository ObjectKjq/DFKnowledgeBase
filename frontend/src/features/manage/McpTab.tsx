import { configText } from '@/mock/logic'
import { useKb } from '@/stores/knowledge'

import { Icon } from '@/components/Icon'

/** MCP 接入 Tab（复刻原型 mcpHTML）。 */
export function McpTab() {
  const credentials = useKb((s) => s.credentials)
  const lastConfigClient = useKb((s) => s.lastConfigClient)
  const setLastConfigClient = useKb((s) => s.setLastConfigClient)
  const openModal = useKb((s) => s.openModal)
  const revokeCredential = useKb((s) => s.revokeCredential)
  const copyText = useKb((s) => s.copyText)

  const config = configText(lastConfigClient)

  return (
    <>
      <div className="info-banner">
        <Icon name="shield" />
        <span>
          每份凭证跟随你的当前权限，默认只读、90 天过期，撤销后即时失效。此处为凭证管理演示，不会建立真实 MCP
          服务。
        </span>
      </div>

      <div className="manage-toolbar">
        <span style={{ fontSize: 12 }}>
          你的连接凭证 <span className="muted">/ {credentials.length}</span>
        </span>
        <button className="btn primary" onClick={() => openModal({ kind: 'credential' })}>
          <Icon name="plus" />
          新建凭证
        </button>
      </div>

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>名称</th>
              <th>工具</th>
              <th>状态</th>
              <th>到期时间</th>
              <th>最后使用</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {credentials.map((c) => (
              <tr key={c.id}>
                <td>{c.name}</td>
                <td title={c.tools.join(', ')}>{c.tools.length} 个只读工具</td>
                <td>
                  <span className={`tag ${c.status === '已撤销' ? 'oat' : ''}`}>{c.status}</span>
                </td>
                <td>{c.expires}</td>
                <td>{c.last}</td>
                <td>
                  {c.status === '有效' ? (
                    <button className="btn small ghost" onClick={() => revokeCredential(c.id)}>
                      撤销
                    </button>
                  ) : (
                    '—'
                  )}
                </td>
              </tr>
            ))}
            {!credentials.length && (
              <tr>
                <td colSpan={6} style={{ textAlign: 'center', padding: 35 }}>
                  还没有凭证，创建一份开始连接。
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <div className="settings-card" style={{ marginTop: 24 }}>
        <h3>
          把知识带到你工作的地方<span className="tag oat">配置示例</span>
        </h3>
        <p>选择客户端，复制配置，并将 YOUR_TOKEN 替换为创建凭证时展示的令牌。端点为本地地址示例。</p>
        <div className="client-tabs">
          {['Codex', 'Trae', 'Claude'].map((c) => (
            <button
              key={c}
              className={lastConfigClient === c ? 'active' : ''}
              onClick={() => setLastConfigClient(c)}
            >
              {c}
            </button>
          ))}
        </div>
        <pre className="code-box" id="config-code">
          {config}
        </pre>
        <div className="form-actions">
          <button className="btn small" onClick={() => void copyText(config)}>
            <Icon name="copy" />
            复制配置
          </button>
        </div>
        <p className="table-note">
          可用只读工具：search · search_images · ask · list_knowledge_bases。实际接入须部署后端服务。
        </p>
      </div>

      <div className="settings-card">
        <h3>
          最近调用<span className="tag">示例记录</span>
        </h3>
        <p style={{ marginBottom: 0 }}>今天 09:41 · search · 我的知识库 · 3 条结果 · 我的 Codex</p>
      </div>
    </>
  )
}
