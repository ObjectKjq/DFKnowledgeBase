/** 尚未接入的 MCP 与模型管理页示例配置。 */
export interface Credential {
  id: string
  name: string
  expires: string
  status: string
  last: string
  tools: string[]
}

export interface ModelCfg {
  name: string
  url: string
  type: string
}

export interface ModelConfig {
  chat: ModelCfg
  embedding: ModelCfg
  rerank: ModelCfg
}

export const STORE_KEY = 'zhixu-kb-front-v1'

export const DEFAULT_CREDENTIALS: Credential[] = [
  {
    id: 'c1',
    name: '我的 Codex',
    expires: '2027-01-14',
    status: '有效',
    last: '今天 09:41',
    tools: ['search', 'search_images', 'ask', 'list_knowledge_bases'],
  },
]

export const DEFAULT_MODEL_CONFIG: ModelConfig = {
  chat: { name: 'Qwen3', url: 'http://localhost:11434/v1', type: '本地模型服务' },
  embedding: { name: 'bge-m3', url: 'http://localhost:8001/v1', type: '本地模型服务' },
  rerank: { name: 'bge-reranker-v2-m3', url: 'http://localhost:8002/v1', type: '本地模型服务' },
}

export const MCP_TOOLS = ['search', 'search_images', 'ask', 'list_knowledge_bases']
