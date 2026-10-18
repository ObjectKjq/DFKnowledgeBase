/**
 * 本地模拟数据与领域类型 —— 逐项复刻原型 `docs/prototype/本地化知识库原型_第一版.html`
 * 中的 defaults / modes / suggestionSets 等常量。当前无真实后端接口，界面由这批模拟数据驱动。
 */

export type LibType = 'personal' | 'shared'

export type DocStatus = '排队' | '解析中' | '索引中' | '可用' | '失败' | '已删除'

export interface Lib {
  id: string
  name: string
  role: string
  type: LibType
  description?: string
}

export interface Doc {
  id: string
  title: string
  ext: string
  lib: string
  status: DocStatus
  date: string
  size: string
  page: number
  pages: number
  section: string
  tags: string
  text: string
  context?: string
  after?: string
  reason?: string
  previousStatus?: DocStatus
  deletedAt?: string
}

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

export type MessageType = 'user' | 'results' | 'answer' | 'guide' | 'empty' | 'note'

export interface Message {
  id: string
  type: MessageType
  /** 用户消息文本 / guide 与 empty 的说明文本 */
  text?: string
  /** 触发该响应的用户查询 */
  query?: string
  /** results / answer：命中的文档 id 列表 */
  ids?: string[]
  scope?: string
  mode?: string
  /** 用户消息携带的查询图片（仅内存，不持久化） */
  image?: string
  /** answer：答案 HTML 片段 */
  answer?: string
  model?: string
  /** 是否附带联网分区 */
  web?: boolean
  /** guide：推荐选项 */
  options?: string[]
  /** guide：本轮引导是否已结束 */
  done?: boolean
  /** empty：是否为 RAG 未找到依据 */
  rag?: boolean
  /** answer：是否有帮助反馈 */
  feedback?: boolean
}

export interface Session {
  id: string
  title: string
  mode: string
  scope: string[]
  skills: string[]
  model: string
  tier: string
  messages: Message[]
}

export const STORE_KEY = 'zhixu-kb-front-v1'

export const DEFAULT_LIBS: Lib[] = [
  { id: 'personal', name: '我的知识库', role: '所有者', type: 'personal' },
  { id: 'design', name: '产品与设计', role: '所有者', type: 'shared' },
  { id: 'research', name: '研究与阅读', role: '查看者', type: 'shared' },
]

export const DEFAULT_DOCS: Doc[] = [
  {
    id: 'd1',
    title: '知识检索与可信答案设计',
    ext: 'pdf',
    lib: 'personal',
    status: '可用',
    date: '2026-10-16',
    size: '2.4 MB',
    page: 12,
    pages: 24,
    section: '03 / 让答案有据可循',
    tags: '知识 检索 RAG 引用 可信 方案 混合 关键字 语义',
    text: '可信的知识问答需要建立完整的证据链。采用关键字与语义融合检索，在答案中为每一项结论附上来源引用，并允许用户直接定位到原文对应的页或分块。',
    context:
      '关键字检索适合编号、术语和精确表达；语义检索能够找回意思相近但措辞不同的内容。两种方式融合排序，可以兼顾准确匹配与模糊回忆。',
    after:
      '当知识库中没有足够依据时，应明确说明未找到足够的支撑资料，引导用户扩大检索范围或补充文档，不生成缺乏出处的答案。',
  },
  {
    id: 'd2',
    title: '渐进式披露：让复杂变简单',
    ext: 'md',
    lib: 'design',
    status: '可用',
    date: '2026-10-15',
    size: '36 KB',
    page: 3,
    pages: 8,
    section: '02 / 在恰当的时刻呈现信息',
    tags: '设计 交互 渐进式披露 复杂 简约 认知 负担',
    text: '渐进式披露将次要功能收纳在需要时才出现的位置。让用户首先看到当前任务最重要的动作，再根据上下文展开更多选项，可以降低认知负担。',
    context:
      '在知识工具里，主要任务是提出问题与阅读结果。模型参数、知识库管理和高级过滤应当让位于这个核心流程。',
    after:
      '输入区只保留范围、当前模式与发送动作。附加能力通过轻量菜单出现，原文预览则按需在侧边展开，保持会话上下文连续。',
  },
  {
    id: 'd3',
    title: '自然与留白 · 设计情绪板',
    ext: 'png',
    lib: 'design',
    status: '可用',
    date: '2026-10-14',
    size: '1.8 MB',
    page: 1,
    pages: 1,
    section: '01 / 视觉参考',
    tags: '图片 设计 极简 参考图 自然 留白 米白 苔绿 情绪板 色彩',
    text: '图片内文字（OCR）：Less, but better. 自然色、纸张肌理与大面积留白，构成安静而有层次的视觉体验。',
    context:
      '色彩记录：米白 #FAFAFA、浅苔绿 #D6DFCA、燕麦 #E5D9C4。图片向量与 OCR 文字可以共同参与图片检索。',
    after: '参考图以“自然”“极简”“留白”为标签，适合用于知识工具的视觉方向探索。',
  },
  {
    id: 'd4',
    title: '个人知识管理方法札记',
    ext: 'pdf',
    lib: 'personal',
    status: '可用',
    date: '2026-10-13',
    size: '1.2 MB',
    page: 5,
    pages: 16,
    section: '02 / 从收集到连接',
    tags: '知识 管理 笔记 学习 方法 收集 整理 阅读 灵感',
    text: '知识管理的价值不在于积累多少文件，而在于需要时能否重新找到并建立连接。以问题组织资料，保留原始出处，并周期性回顾，比建立过度复杂的目录更有效。',
    context: '收集是一切的起点，但收集后需要建立清晰的命名和基本标签。',
    after: '尝试在完成每次阅读后记录一个问题与一条结论，并把结论连接到原文。',
  },
  {
    id: 'd5',
    title: '本地 RAG 检索架构笔记',
    ext: 'md',
    lib: 'research',
    status: '可用',
    date: '2026-10-12',
    size: '52 KB',
    page: 2,
    pages: 9,
    section: '01 / 混合检索与重排',
    tags: '本地 RAG 检索 知识库 方案 模型 向量 重排 MCP',
    text: '本地知识库通过解析、分块、建立关键字与向量索引完成入库。检索时融合两路结果，再用重排模型精排；生成答案时，仅使用当前用户有权访问且已完成索引的内容。',
    context: '资料入库经历排队、解析中、索引中和可用四个状态。只有可用文档参与检索。',
    after: 'MCP 服务复用同一套检索与权限规则，使外部智能体能够在授权范围内访问知识。',
  },
  {
    id: 'd6',
    title: '阅读摘录 · 信息的秩序',
    ext: 'txt',
    lib: 'personal',
    status: '可用',
    date: '2026-10-11',
    size: '18 KB',
    page: 1,
    pages: 4,
    section: '01 / 阅读摘录',
    tags: '阅读 信息 秩序 知识 笔记 灵感',
    text: '为信息建立秩序，是为未来的自己保留一条回到灵感的路。清晰的出处、恰当的标签和轻量的整理方式，让知识可以在不同语境里重新被使用。',
    context: '每一份资料都有被重新发现的价值。',
    after: '从一个小而稳定的收集与回顾习惯开始。',
  },
  {
    id: 'd7',
    title: '课程板书扫描件',
    ext: 'pdf',
    lib: 'personal',
    status: '失败',
    date: '2026-10-16',
    size: '5.3 MB',
    page: 1,
    pages: 6,
    section: '扫描件',
    tags: '课程 板书',
    text: '',
    reason: '扫描质量过低，部分文字无法识别。可重试或上传更清晰的文件。',
  },
  {
    id: 'd8',
    title: '多模态检索论文摘录',
    ext: 'pdf',
    lib: 'research',
    status: '可用',
    date: '2026-10-10',
    size: '3.5 MB',
    page: 4,
    pages: 12,
    section: '02 / 图文联合检索',
    tags: '图片 多模态 OCR 检索 以文搜图 以图搜图 向量',
    text: '图内文字检索使用 OCR 结果定位文字所在图片；以文搜图和以图搜图通过多模态向量匹配内容与视觉相似性。结果需保留图片缩略图与原始来源定位。',
    context: '图片是知识资料的重要组成，包括截图、照片和独立图表。',
    after: '向量模型不可用时，应明确提示图片向量检索不可用，仍保留可用的精确文字检索入口。',
  },
]

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

/** 检索模式：[模式名, 说明]。 */
export const MODES: [string, string][] = [
  ['直接检索', '关键字 + 语义，融合查找'],
  ['精确检索', '按术语、编号和字面精确匹配'],
  ['模糊检索', '凭模糊记忆，按语义寻找'],
  ['引导检索', '先澄清需求，再帮你找到'],
  ['RAG 检索', '基于资料回答，逐条附上引用'],
]

export const SUGGESTION_SETS: string[][] = [
  ['帮我找关于知识管理的笔记', '如何让知识检索更可信？', '找一张极简设计的参考图'],
  ['渐进式披露是什么？', '本地 RAG 是如何检索的？', '图片里的文字能被找到吗？'],
]

/** 首屏英雄插画（原型内联 SVG）。 */
export const HERO_ART = `<svg class="hero-art" viewBox="0 0 190 127" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true"><ellipse cx="97" cy="111" rx="69" ry="7" fill="#e8ebdf" stroke="none"/><circle cx="101" cy="59" r="49" fill="#f0f2e9" stroke="none"/><path d="M39 88 94 75l56 20-53 17-58-20Z" fill="#d4deca" stroke="#c0ceb2" stroke-width=".8"/><path d="m42 86 55 18 50-13v5l-50 15-55-18Z" fill="#e5ebda" stroke="#c8d4ba" stroke-width=".8"/><path d="M46 78 94 64l52 19-50 16-50-16Z" fill="#f9faf5" stroke="#cbd4be" stroke-width=".8"/><path d="m96 99 0 5m-43-22 37 12m-30-16 34 11m8 2 34-11" stroke="#c6d1b9" stroke-width=".8"/><path d="m61 69 1-43 36 10 34-11v43L98 82Z" fill="#fbfcf6" stroke="#b8c7a5" stroke-width="1"/><path d="M98 36v46m-30-49 23 7m-23 0 23 7m-23 0 16 5m21-11 20-7m-20 14 20-7m-20 14 13-4" stroke="#c8d4b9" stroke-width="1"/><path d="M98 79c-10-12-15-28-15-46" stroke="#84996d" stroke-width="1.2"/><path d="M83 48C67 50 63 36 66 29c11 1 21 8 17 19Z" fill="#bdcda9" stroke="#9eaf87" stroke-width=".8"/><path d="M84 38c-2-12 7-22 17-24 3 11-1 22-17 24Z" fill="#d0ddbd" stroke="#a9bc91" stroke-width=".8"/><path d="m83 48-10-10m12-1 9-14" stroke="#93a679" stroke-width=".7"/><path d="M137 66c8-3 13-9 15-17m-11 13c-3-10 3-17 11-19 2 8-1 15-11 19Z" fill="#d9e3cc" stroke="#afc09a" stroke-width=".8"/><circle cx="47" cy="45" r="2.5" fill="#d4dec6" stroke="none"/><path d="M143 29h5m-2.5-2.5v5M38 69h4m-2-2v4" stroke="#b2c39c" stroke-width="1"/></svg>`

/** 图片结果与文档预览使用的情绪板插画（原型内联 SVG）。 */
export const IMAGE_ART = `<svg viewBox="0 0 240 160" xmlns="http://www.w3.org/2000/svg" aria-label="极简设计情绪板"><rect width="240" height="160" fill="#e9ede2" stroke="none"/><rect x="24" y="25" width="77" height="111" rx="2" fill="#f9f8f1" stroke="#d7dec9"/><rect x="115" y="25" width="99" height="62" rx="2" fill="#d6dfca" stroke="none"/><rect x="115" y="99" width="43" height="37" rx="2" fill="#c0cdb3" stroke="none"/><rect x="169" y="99" width="45" height="37" rx="2" fill="#e5d9c4" stroke="none"/><path d="M40 48h40m-40 9h28m-28 50h40m-40 8h28M148 77c-10-21 7-44 33-40-4 21-12 33-33 40Zm0 0 25-29" fill="none" stroke="#8d9f7b" stroke-width="1.2"/></svg>`
