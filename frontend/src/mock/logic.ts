/**
 * 原型检索与文案逻辑的纯函数移植 —— 与 `docs/prototype/...html` 中
 * retrieve / highlight / buildAnswer / config 等函数一一对应，保持行为一致。
 */
import type { Doc, Lib } from './data'

export const uuid = () => Date.now().toString(36) + Math.random().toString(36).slice(2, 7)

export function formatSize(n: number) {
  return n > 1048576 ? (n / 1048576).toFixed(1) + ' MB' : Math.max(1, Math.round(n / 1024)) + ' KB'
}

export function libName(libs: Lib[], id: string) {
  return libs.find((x) => x.id === id)?.name || '未知知识库'
}

export function writable(libs: Lib[], id: string) {
  return libs.find((x) => x.id === id)?.role !== '查看者'
}

/** 判断扩展名是否为图片。 */
export function isImage(ext: string) {
  return ext === 'png' || ext === 'jpg' || ext === 'jpeg' || ext === 'webp' || ext === 'bmp'
}

const LEXICON = [
  '渐进式披露', '渐进', '披露', '认知', '负担', '设计', '简约', '极简', '留白', '色彩', '参考图',
  '图片', '图内文字', 'ocr', '多模态', '向量', '语义', '关键字', '混合', '重排', '可信', '引用',
  '出处', '证据', 'rag', 'mcp', '本地', '知识管理', '管理', '笔记', '整理', '学习', '阅读', '检索',
  '模型', '扫描', '板书',
]

/** 检索：直接/精确/模糊/图片。与原型 retrieve 行为一致。 */
export function retrieve(
  pool: Doc[],
  query: string,
  mode: string,
  skills: string[],
  imageOnly: boolean,
): Doc[] {
  let p = pool
  const s = query.toLowerCase().trim()
  if (skills.includes('图片搜索')) p = p.filter((d) => d.ext === 'png' || d.tags.includes('图片'))
  if (imageOnly) return p.filter((d) => d.ext === 'png').slice(0, 3)
  if (!s) return []
  const cleaned = s.replace(
    /帮我|找一下|帮忙|我想|请问|关于|相关资料|帮|我的|一张|一个|如何|什么|哪些|怎么|可以|有没有|请|[？?]/g,
    '',
  ).trim()
  if (mode === '精确检索') {
    return p.filter((d) => (d.title + d.text + d.tags).toLowerCase().includes(cleaned)).slice(0, 5)
  }
  let tokens = LEXICON.filter((t) => s.includes(t))
  if (!tokens.length) tokens = s.match(/[a-z0-9]{2,}/g) || []
  if (/可信|出处|证据|引用/.test(s)) tokens.push('可信', '引用', '证据')
  if (/简单|复杂|认知/.test(s)) tokens.push('渐进', '披露', '认知')
  if (/图片|参考图|以图|画面|极简|截图/.test(s)) tokens.push('图片', '极简')
  if (/记忆|回忆|模糊/.test(s)) tokens.push('语义')
  if (/知识管理|整理|学习/.test(s)) tokens.push('管理', '笔记')
  tokens = [...new Set(tokens)]
  return p
    .map((d) => ({
      d,
      score: tokens.reduce(
        (n, t) =>
          n +
          (d.title.toLowerCase().includes(t) ? 4 : d.tags.toLowerCase().includes(t) ? 2 : d.text.toLowerCase().includes(t) ? 1 : 0),
        0,
      ),
    }))
    .filter((x) => x.score > 0)
    .sort((a, b) => b.score - a.score)
    .slice(0, mode === '模糊检索' ? 5 : 3)
    .map((x) => x.d)
}

/** 是否命中「可信/引用/证据/出处」这一类答案模板。 */
export function isTrustQuery(query: string) {
  return /可信|引用|证据|出处/.test(query)
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
