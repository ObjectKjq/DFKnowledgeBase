/** 格式化工具与尚未接入的 MCP 配置示例。 */
import type { Library } from '@/features/knowledge/libraries'

export const uuid = () => Date.now().toString(36) + Math.random().toString(36).slice(2, 7)

export function formatSize(n: number) {
  return n > 1048576 ? (n / 1048576).toFixed(1) + ' MB' : Math.max(1, Math.round(n / 1024)) + ' KB'
}

export function libName(libs: Library[], id: string) {
  return libs.find((x) => x.id === id)?.name || '未知知识库'
}

/** MCP 客户端配置文本。 */
export function configText(client: string, token = 'YOUR_TOKEN') {
  if (client === 'Codex') {
    return `[mcp_servers.zhixu]\nurl = "http://localhost:8000/mcp"\nhttp_headers = { Authorization = "Bearer ${token}" }`
  }
  return JSON.stringify(
    {
      mcpServers: {
        zhixu: {
          url: 'http://localhost:8000/mcp',
          headers: { Authorization: 'Bearer ' + token },
        },
      },
    },
    null,
    2,
  )
}
