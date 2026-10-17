/**
 * 全局模拟数据 store —— 复刻原型脚本中的状态与行为（defaults/current/busy/guide/preview…）。
 * 后端接口尚未实现，本 store 以前端本地数据驱动整套交互；已持久化的部分仅为领域数据。
 */
import { create } from 'zustand'
import { persist } from 'zustand/middleware'

import {
  DEFAULT_CREDENTIALS,
  DEFAULT_DOCS,
  DEFAULT_LIBS,
  DEFAULT_MEMBERS,
  DEFAULT_MODEL_CONFIG,
  MCP_TOOLS,
  STORE_KEY,
  type Credential,
  type Doc,
  type DocStatus,
  type Lib,
  type LibType,
  type Member,
  type Message,
  type ModelConfig,
  type Session,
} from '@/mock/data'
import { configText, isTrustQuery, libName, retrieve, uuid } from '@/mock/logic'

const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms))

export interface Attachment {
  name: string
  image?: boolean
  dataUrl?: string
}

export interface PendingFile {
  name: string
  size: number
}

export interface GuideState {
  query: string
  answers: string[]
  round: number
}

export type ModalState =
  | { kind: 'upload' }
  | { kind: 'credential' }
  | { kind: 'token'; token: string }
  | { kind: 'library' }
  | { kind: 'invite' }
  | { kind: 'vector-confirm' }
  | { kind: 'copy'; text: string }
  | { kind: 'confirm'; title: string; text: string; confirmLabel: string }

const NEW_SESSION = (): Session => ({
  id: 'new',
  title: '新检索',
  mode: '直接检索',
  scope: [],
  skills: [],
  model: 'Qwen3 · 本地',
  tier: '均衡',
  messages: [],
})

interface KbState {
  // 领域数据
  libs: Lib[]
  docs: Doc[]
  credentials: Credential[]
  members: Member[]
  sessions: Session[]
  modelConfig: ModelConfig

  // 会话
  current: Session
  draft: string
  busy: boolean
  guide: GuideState | null
  attachment: Attachment | null
  suggestionIndex: number

  // 弹层 / 抽屉 / 提示
  popover: string
  previewId: string | null
  previewPage: number
  contextExpanded: boolean
  modal: ModalState | null
  modalConfirm: (() => void) | null
  toast: { msg: string; error: boolean } | null

  // 管理页
  docFilter: string
  docSearch: string
  memberLib: string
  lastConfigClient: string

  // 上传
  pendingUpload: PendingFile[]
  uploadTarget: string
  uploadStarted: boolean
  oneTimeToken: string | null

  // 操作
  toastMsg: (msg: string, error?: boolean) => void
  clearToast: () => void
  setDraft: (v: string) => void
  syncSession: () => void
  newChat: () => void

  openPopover: (name: string) => void
  closePopover: () => void

  setScope: (ids: string[]) => void
  toggleScope: (id: string, checked: boolean) => void
  setMode: (mode: string) => void
  setModel: (model: string) => void
  setTier: (tier: string) => void
  toggleSkill: (skill: string) => void
  removeSkill: (skill: string) => void

  setAttachment: (a: Attachment | null) => void
  setImage: (file: File) => void
  shuffleSuggestions: () => void

  sendQuery: (override?: string) => Promise<void>
  executeQuery: (query: string, mode: string, scope: string) => Promise<void>
  advanceGuide: () => Promise<void>
  skipGuide: () => Promise<void>
  broaden: (query: string) => Promise<void>
  toggleFeedback: (id: string) => void
  openHistory: (id: string) => void

  showPreview: (id: string) => void
  closePreview: () => void
  previewPageTo: (page: number) => void
  toggleContext: () => void
  previewNew: () => void

  openModal: (m: ModalState, confirm?: () => void) => void
  closeModal: () => void

  setDocFilter: (v: string) => void
  setDocSearch: (v: string) => void
  setMemberLib: (v: string) => void
  setMemberRole: (name: string, role: string) => void

  deleteDoc: (id: string) => void
  restoreDoc: (id: string) => void
  purgeDoc: (id: string) => void
  retryDoc: (id: string) => Promise<void>

  createCredential: (name: string, days: number) => void
  revokeCredential: (id: string) => void

  createLibrary: (name: string, description: string) => void
  deleteLibrary: (id: string) => void
  inviteMember: (name: string, role: string) => void
  removeMember: (name: string) => void

  openUpload: () => void
  addPendingFiles: (files: { name: string; size: number }[]) => void
  removePending: (index: number) => void
  setUploadTarget: (id: string) => void
  startUpload: () => Promise<void>

  saveModelConfig: (kind: keyof ModelConfig, cfg: { name: string; url: string; type: string }) => void
  setLastConfigClient: (c: string) => void

  copyText: (text: string) => Promise<void>
  resetDemo: () => void
  confirmThen: (title: string, text: string, confirmLabel: string, fn: () => void) => void
}

let toastTimer: ReturnType<typeof setTimeout> | undefined

export const useKb = create<KbState>()(
  persist(
    (set, get) => ({
      libs: structuredClone(DEFAULT_LIBS),
      docs: structuredClone(DEFAULT_DOCS),
      credentials: structuredClone(DEFAULT_CREDENTIALS),
      members: structuredClone(DEFAULT_MEMBERS),
      sessions: [],
      modelConfig: structuredClone(DEFAULT_MODEL_CONFIG),

      current: NEW_SESSION(),
      draft: '',
      busy: false,
      guide: null,
      attachment: null,
      suggestionIndex: 0,

      popover: '',
      previewId: null,
      previewPage: 1,
      contextExpanded: false,
      modal: null,
      modalConfirm: null,
      toast: null,

      docFilter: 'all',
      docSearch: '',
      memberLib: 'design',
      lastConfigClient: 'Codex',

      pendingUpload: [],
      uploadTarget: 'personal',
      uploadStarted: false,
      oneTimeToken: null,

      toastMsg: (msg, error = false) => {
        set({ toast: { msg, error } })
        clearTimeout(toastTimer)
        toastTimer = setTimeout(() => set({ toast: null }), 4200)
      },
      clearToast: () => set({ toast: null }),
      setDraft: (v) => set({ draft: v }),

      syncSession: () => {
        const { current, sessions } = get()
        if (!current.messages.length) return
        const c: Session = {
          ...current,
          messages: current.messages.map((m) => {
            const copy = { ...m }
            delete copy.image
            return copy
          }),
        }
        const next = sessions.filter((s) => s.id !== current.id)
        next.unshift(c)
        set({ sessions: next.slice(0, 16) })
      },

      newChat: () => {
        if (get().busy) {
          get().toastMsg('请等待当前演示检索完成')
          return
        }
        get().syncSession()
        set({
          current: { ...NEW_SESSION(), id: uuid() },
          draft: '',
          guide: null,
          attachment: null,
          previewId: null,
          contextExpanded: false,
          popover: '',
        })
      },

      openPopover: (name) => set({ popover: get().popover === name ? '' : name }),
      closePopover: () => set({ popover: '' }),

      setScope: (ids) => {
        set({ current: { ...get().current, scope: ids } })
      },
      toggleScope: (id, checked) => {
        const { current } = get()
        const scope = checked ? [...current.scope, id] : current.scope.filter((x) => x !== id)
        set({ current: { ...current, scope } })
      },
      setMode: (mode) => {
        set({ current: { ...get().current, mode } })
        get().syncSession()
      },
      setModel: (model) => {
        set({ current: { ...get().current, model } })
        get().syncSession()
      },
      setTier: (tier) => {
        set({ current: { ...get().current, tier } })
        get().syncSession()
      },
      toggleSkill: (skill) => {
        const { current } = get()
        const skills = current.skills.includes(skill)
          ? current.skills.filter((s) => s !== skill)
          : [...current.skills, skill]
        set({ current: { ...current, skills } })
        get().syncSession()
      },
      removeSkill: (skill) => {
        set({ current: { ...get().current, skills: get().current.skills.filter((s) => s !== skill) } })
        get().syncSession()
      },

      setAttachment: (a) => set({ attachment: a }),
      setImage: (file) => {
        if (!file || !/^image\/(png|jpeg|webp|bmp)$/.test(file.type)) {
          get().toastMsg('请选择 PNG、JPG、WebP 或 BMP 图片', true)
          return
        }
        if (file.size > 10 * 1024 * 1024) {
          get().toastMsg('原型预览支持 10 MB 以内的查询图片', true)
          return
        }
        const reader = new FileReader()
        reader.onload = () => {
          const { current } = get()
          const skills = current.skills.includes('图片搜索')
            ? current.skills
            : [...current.skills, '图片搜索']
          set({
            attachment: { name: file.name, image: true, dataUrl: reader.result as string },
            current: { ...current, skills },
          })
          get().closePopover()
          get().toastMsg('已添加查询图片，可补充描述后发送；相似结果为预置演示')
        }
        reader.readAsDataURL(file)
      },
      shuffleSuggestions: () => set({ suggestionIndex: get().suggestionIndex + 1 }),

      sendQuery: async (override) => {
        const { busy, draft, attachment, current } = get()
        if (busy) return
        const q = (override ?? draft).trim()
        if (!q && !attachment) {
          get().toastMsg('先写下你想找的内容')
          return
        }
        if (q.length > 2000) {
          get().toastMsg('问题较长，请控制在 2000 字以内', true)
          return
        }
        let session = current
        if (session.id === 'new') {
          session = { ...session, id: uuid() }
          set({ current: session })
        }
        const query = q || (attachment ? '查找与这张图片相似的内容' : '')
        const scope = session.scope.length
          ? session.scope.map((l) => libName(get().libs, l)).join('、')
          : '全部知识库'
        const title = session.title === '新检索' ? query.slice(0, 22) : session.title
        const userMsg: Message = {
          id: uuid(),
          type: 'user',
          text: query,
          scope,
          mode: get().guide ? '引导补充' : session.mode,
          image: attachment?.dataUrl,
        }
        set({
          current: { ...session, title, messages: [...session.messages, userMsg] },
          draft: '',
        })
        const guide = get().guide
        if (guide) {
          set({ guide: { ...guide, answers: [...guide.answers, query] } })
          await get().advanceGuide()
          return
        }
        if (session.mode === '引导检索') {
          set({ guide: { query, answers: [], round: 1 } })
          set({
            current: {
              ...get().current,
              messages: [
                ...get().current.messages,
                {
                  id: uuid(),
                  type: 'guide',
                  text: '为了更快找到你需要的资料，你更想了解哪一类内容？',
                  options: ['概念与定义', '方法与实践', '图片与图表'],
                },
              ],
            },
            attachment: null,
          })
          get().syncSession()
          return
        }
        await get().executeQuery(query, session.mode, scope)
      },

      executeQuery: async (query, mode, scope) => {
        set({ busy: true, popover: '' })
        const imageQuery = !!get().attachment?.image
        const pool = get().docs.filter(
          (d) => d.status === '可用' && (!get().current.scope.length || get().current.scope.includes(d.lib)),
        )
        let found = retrieve(pool, query, mode, get().current.skills, imageQuery).filter(
          (d) => mode !== 'RAG 检索' || d.text,
        )
        if (mode === 'RAG 检索' && isTrustQuery(query) && found.some((d) => d.id === 'd1')) {
          found = found.filter((d) => ['d1', 'd5'].includes(d.id))
        }
        const responseModel = get().current.model
        const responseWeb = get().current.skills.includes('联网搜索')
        set({ attachment: null })
        await sleep(650)
        const hasContent = get().docs.some(
          (d) => d.status === '可用' && (!get().current.scope.length || get().current.scope.includes(d.lib)),
        )
        const ids = found.map((d) => d.id)
        let msg: Message
        if (!found.length) {
          msg = {
            id: uuid(),
            type: 'empty',
            query,
            rag: mode === 'RAG 检索',
            text: !hasContent
              ? '当前知识库暂无可用内容。上传资料并完成索引后，再开始检索。'
              : mode === 'RAG 检索'
                ? '当前示例资料不足以支持这个问题，因此不生成答案。可扩大范围、调整问法，或补充相关文档。'
                : '试试更短的关键词、切换模糊检索，或检查当前知识库范围。',
          }
        } else if (mode === 'RAG 检索') {
          msg = { id: uuid(), type: 'answer', query, ids, scope, model: responseModel, web: responseWeb }
        } else {
          msg = {
            id: uuid(),
            type: 'results',
            query,
            ids,
            scope,
            mode: imageQuery ? '以图搜图 · 示例相似结果' : mode,
            web: responseWeb,
          }
        }
        set({
          current: { ...get().current, messages: [...get().current.messages, msg] },
          busy: false,
          guide: null,
        })
        get().syncSession()
      },

      advanceGuide: async () => {
        const { current, guide } = get()
        if (!guide) return
        const lastGuide = current.messages.filter((m) => m.type === 'guide').at(-1)
        const messages = current.messages.map((m) => (m === lastGuide ? { ...m, done: true } : m))
        if (guide.round === 1) {
          set({
            guide: { ...guide, round: 2 },
            current: {
              ...get().current,
              messages: [
                ...messages,
                {
                  id: uuid(),
                  type: 'guide',
                  text: '还想按哪种方式收窄范围？也可以直接输入更多线索。',
                  options: ['优先查我的知识库', '优先查产品与设计', '保持当前范围'],
                },
              ],
            },
          })
          get().syncSession()
          return
        }
        const g = guide
        let scope = get().current.scope
        let skills = get().current.skills
        if (g.answers.some((a) => a.includes('我的知识库'))) scope = ['personal']
        if (g.answers.some((a) => a.includes('产品与设计'))) scope = ['design']
        if (g.answers.some((a) => a.includes('图片'))) skills = [...new Set([...skills, '图片搜索'])]
        const merged = g.query + ' ' + g.answers.filter((a) => !a.includes('范围') && !a.includes('优先')).join(' ')
        const scopeLabel = scope.length ? scope.map((l) => libName(get().libs, l)).join('、') : '全部知识库'
        set({ guide: null, current: { ...get().current, messages, scope, skills } })
        await get().executeQuery(merged, '直接检索', scopeLabel)
      },

      skipGuide: async () => {
        const { guide, current } = get()
        if (!guide) return
        if (current.messages.find((m) => m.type === 'guide' && !m.done) === undefined) return
        const messages = current.messages.map((m) => (m.type === 'guide' ? { ...m, done: true } : m))
        const scope = current.scope.length
          ? current.scope.map((l) => libName(get().libs, l)).join('、')
          : '全部知识库'
        set({ guide: null, current: { ...current, messages } })
        await get().executeQuery(guide.query, '直接检索', scope)
      },

      broaden: async (query) => {
        set({ current: { ...get().current, scope: [], mode: '模糊检索', skills: [] } })
        await get().sendQuery(query)
      },

      toggleFeedback: (id) => {
        const { current } = get()
        let next = false
        const messages = current.messages.map((m) => {
          if (m.id !== id) return m
          next = !m.feedback
          return { ...m, feedback: next }
        })
        set({ current: { ...current, messages } })
        get().syncSession()
        get().toastMsg(next ? '已记录“有帮助”的反馈' : '已撤回反馈')
      },

      openHistory: (id) => {
        if (get().busy) {
          get().toastMsg('请等待当前检索完成')
          return
        }
        get().syncSession()
        const old = get().sessions.find((s) => s.id === id)
        if (old) {
          const messages = old.messages.map((m) => (m.type === 'guide' ? { ...m, done: true } : m))
          set({
            current: structuredClone({ ...old, messages }),
            guide: null,
            attachment: null,
            popover: '',
            previewId: null,
            contextExpanded: false,
          })
        } else {
          const titles: Record<string, string> = {
            'demo-rag': '如何让知识检索更可信？',
            'demo-design': '关于渐进式披露的设计原则',
            'demo-image': '找一张极简设计的参考图',
          }
          const demoIds: Record<string, string[]> = {
            'demo-rag': ['d1', 'd5'],
            'demo-design': ['d2'],
            'demo-image': ['d3'],
          }
          const title = titles[id] ?? '新检索'
          const mode = id === 'demo-rag' ? 'RAG 检索' : '直接检索'
          const sc = { id, title, mode, scope: [] as string[], skills: [], model: 'Qwen3 · 本地', tier: '均衡', messages: [] as Message[] }
          const clean = (demoIds[id] ?? []).map((x) => get().docs.find((d) => d.id === x)).filter(Boolean) as Doc[]
          const msgs: Message[] = [
            { id: uuid(), type: 'user', text: title, scope: '全部知识库', mode },
            id === 'demo-rag'
              ? { id: uuid(), type: 'answer', query: title, ids: clean.map((d) => d.id), scope: '全部知识库', model: sc.model }
              : {
                  id: uuid(),
                  type: 'results',
                  query: title,
                  ids: clean.map((d) => d.id),
                  scope: '全部知识库',
                  mode: '直接检索',
                },
          ]
          set({ current: { ...sc, messages: msgs }, guide: null, attachment: null, popover: '', previewId: null })
        }
        get().syncSession()
      },

      showPreview: (id) => {
        const d = get().docs.find((x) => x.id === id)
        if (!d) {
          get().toastMsg('原文已删除或无权访问', true)
          return
        }
        if (d.status === '已删除') {
          get().toastMsg('原文已删除，可在文档管理的回收站中恢复', true)
          return
        }
        if (!get().libs.some((l) => l.id === d.lib)) {
          get().toastMsg('无权访问该原文', true)
          return
        }
        set({ previewId: id, previewPage: d.page, contextExpanded: false })
      },
      closePreview: () => set({ previewId: null, contextExpanded: false }),
      previewPageTo: (page) => {
        const d = get().docs.find((x) => x.id === get().previewId)
        if (!d) return
        set({ previewPage: Math.min(Math.max(page, 1), d.pages) })
      },
      toggleContext: () => set({ contextExpanded: !get().contextExpanded }),
      previewNew: () => {
        const d = get().docs.find((x) => x.id === get().previewId)
        if (!d) return
        const win = window.open('about:blank', '_blank')
        if (!win) {
          get().toastMsg('浏览器阻止了新页，请允许弹出窗口后重试', true)
          return
        }
        win.opener = null
        win.document.write(
          `<!DOCTYPE html><html lang="zh-CN"><meta charset="utf-8"><title>${d.title} · 示例原文</title><body style="max-width:720px;margin:60px auto;padding:25px;color:#45573c;background:#fafbf7;font:16px/2 Georgia,'Microsoft YaHei',serif"><p style="font-size:12px;color:#869477">知序 / 原型示例资料 / 第 ${d.page} 页</p><h1>${d.title}</h1><p>${d.context || ''}</p><blockquote style="border-left:3px solid #a8bb93;padding:20px;background:#ecf1e3">${d.text || '当前上传文件未接入真实内容解析。'}</blockquote><p>${d.after || ''}</p><p style="font-size:12px;color:#869477">这里展示预置片段，非实际 PDF 或 Office 渲染。</p></body></html>`,
        )
        win.document.close()
      },

      openModal: (m, confirm) => set({ modal: m, modalConfirm: confirm ?? null }),
      closeModal: () => {
        if (get().uploadStarted) {
          get().toastMsg('正在演示入库，请稍候')
          return
        }
        set({ modal: null, modalConfirm: null, oneTimeToken: null, pendingUpload: [] })
      },

      setDocFilter: (v) => set({ docFilter: v }),
      setDocSearch: (v) => set({ docSearch: v }),
      setMemberLib: (v) => set({ memberLib: v }),
      setMemberRole: (name, role) => {
        const { members, memberLib } = get()
        set({ members: members.map((m) => (m.lib === memberLib && m.name === name ? { ...m, role } : m)) })
        get().toastMsg('成员角色已更新（演示）')
      },

      deleteDoc: (id) => {
        const d = get().docs.find((x) => x.id === id)
        if (!d) return
        get().confirmThen('删除文档', `「${d.title}」将移至回收站，30 天内可恢复。`, '确认删除', () => {
          set({
            docs: get().docs.map((x) =>
              x.id === id
                ? { ...x, previousStatus: x.status, status: '已删除' as DocStatus, deletedAt: '2026-10-16' }
                : x,
            ),
          })
          get().toastMsg('已移至回收站')
        })
      },
      restoreDoc: (id) => {
        set({
          docs: get().docs.map((x) =>
            x.id === id ? { ...x, status: (x.previousStatus || '可用') as DocStatus } : x,
          ),
        })
        get().toastMsg('文档已恢复')
      },
      purgeDoc: (id) => {
        get().confirmThen('彻底删除文档', '这将从原型本地存储中移除此文件记录，无法恢复。', '确认删除', () => {
          set({ docs: get().docs.filter((x) => x.id !== id) })
          get().toastMsg('已彻底删除')
        })
      },
      retryDoc: async (id) => {
        const d = get().docs.find((x) => x.id === id)
        if (!d || d.status !== '失败') return
        for (const status of ['排队', '解析中', '索引中', '可用'] as DocStatus[]) {
          if (get().docs.find((x) => x.id === id)?.status === '已删除') return
          set({ docs: get().docs.map((x) => (x.id === id ? { ...x, status } : x)) })
          await sleep(500)
        }
        get().toastMsg('已完成模拟重试。实际文件解析结果取决于后端服务。')
      },

      createCredential: (name, days) => {
        const date = new Date('2026-10-16T12:00:00+08:00')
        date.setDate(date.getDate() + days)
        const cred: Credential = {
          id: uuid(),
          name,
          expires: date.toISOString().slice(0, 10),
          status: '有效',
          last: '尚未使用',
          tools: [...MCP_TOOLS],
        }
        const token = 'DEMO_ONLY_' + uuid() + '_' + uuid()
        set({ credentials: [...get().credentials, cred], oneTimeToken: token, modal: { kind: 'token', token } })
      },
      revokeCredential: (id) => {
        get().confirmThen('撤销凭证', '撤销后，该凭证将立即无法继续调用。此操作不可恢复。', '确认撤销', () => {
          set({ credentials: get().credentials.map((c) => (c.id === id ? { ...c, status: '已撤销' } : c)) })
          get().toastMsg('凭证已撤销（演示）')
        })
      },

      createLibrary: (name, description) => {
        const l: Lib = { id: uuid(), name, type: 'shared' as LibType, role: '所有者', description }
        set({
          libs: [...get().libs, l],
          members: [...get().members, { name: '林予', role: '所有者', lib: l.id }],
          memberLib: l.id,
        })
        get().closeModal()
        get().toastMsg('共享知识库已创建，可添加演示成员')
      },
      deleteLibrary: (id) => {
        if (get().libs.find((l) => l.id === id)?.role !== '所有者') return
        const name = libName(get().libs, id)
        get().confirmThen('删除共享知识库', `「${name}」的内容将对所有成员不可见。请确认已保留需要的资料。`, '确认删除', () => {
          set({
            libs: get().libs.filter((l) => l.id !== id),
            docs: get().docs.filter((d) => d.lib !== id),
            members: get().members.filter((m) => m.lib !== id),
            current: { ...get().current, scope: get().current.scope.filter((s) => s !== id) },
          })
          get().toastMsg('共享知识库已删除')
        })
      },
      inviteMember: (name, role) => {
        const { members, memberLib } = get()
        if (members.some((m) => m.lib === memberLib && m.name === name)) {
          get().toastMsg('该成员已在知识库中', true)
          return
        }
        set({ members: [...members, { name, role, lib: memberLib }] })
        get().closeModal()
        get().toastMsg('演示成员已添加')
      },
      removeMember: (name) => {
        const { memberLib } = get()
        get().confirmThen('移除成员', `移除「${name}」后，该成员与其 MCP 凭证将无法访问此共享库。`, '确认移除', () => {
          set({ members: get().members.filter((m) => !(m.lib === memberLib && m.name === name)) })
          get().toastMsg('演示成员已移除')
        })
      },

      openUpload: () => {
        const { current, libs } = get()
        const target =
          current.scope.length === 1 && libs.find((l) => l.id === current.scope[0])?.role !== '查看者'
            ? current.scope[0]
            : 'personal'
        set({ pendingUpload: [], uploadTarget: target, uploadStarted: false, modal: { kind: 'upload' } })
      },
      addPendingFiles: (files) => {
        const pending = [...get().pendingUpload]
        for (const f of files) {
          if (!pending.some((p) => p.name === f.name && p.size === f.size)) pending.push(f)
        }
        set({ pendingUpload: pending })
      },
      removePending: (index) => set({ pendingUpload: get().pendingUpload.filter((_, i) => i !== index) }),
      setUploadTarget: (id) => set({ uploadTarget: id }),
      startUpload: async () => {
        const { pendingUpload, uploadTarget, uploadStarted } = get()
        if (!pendingUpload.length || uploadStarted) return
        const targetLib = get().libs.find((l) => l.id === uploadTarget)
        if (!targetLib || targetLib.role === '查看者') {
          get().toastMsg('当前知识库没有上传权限', true)
          return
        }
        set({ uploadStarted: true })
        const fresh: Doc[] = pendingUpload.map((f) => ({
          id: uuid(),
          title: f.name.replace(/\.[^.]+$/, ''),
          ext: (f.name.split('.').pop() || '').toLowerCase(),
          lib: uploadTarget,
          status: '排队' as DocStatus,
          date: '2026-10-16',
          size: (f.size > 1048576 ? (f.size / 1048576).toFixed(1) + ' MB' : Math.max(1, Math.round(f.size / 1024)) + ' KB'),
          page: 1,
          pages: 1,
          section: '上传文件 · 原型演示',
          tags: f.name,
          text: '',
          context: '文件已在浏览器中完成模拟入库。当前原型不解析实际文件正文。',
          after: '可体验状态流转、删除、恢复与列表筛选。真实内容检索需要接入解析、索引和模型服务。',
        }))
        set({ docs: [...get().docs, ...fresh] })
        for (const state of ['解析中', '索引中', '可用'] as DocStatus[]) {
          await sleep(700)
          set({ docs: get().docs.map((d) => (fresh.some((f) => f.id === d.id) ? { ...d, status: state } : d)) })
        }
        set({ uploadStarted: false, modal: null, pendingUpload: [] })
        get().toastMsg(`${pendingUpload.length} 份资料已完成模拟入库；未解析实际正文`)
      },

      saveModelConfig: (kind, cfg) => {
        set({ modelConfig: { ...get().modelConfig, [kind]: cfg } })
        if (kind === 'chat') {
          set({ current: { ...get().current, model: cfg.name + ' · ' + (cfg.type === '本地模型服务' ? '本地' : '私有服务') } })
        }
        get().toastMsg('模型演示配置已保存，未连接实际服务')
      },
      setLastConfigClient: (c) => set({ lastConfigClient: c }),

      copyText: async (text) => {
        try {
          await navigator.clipboard.writeText(text)
          get().toastMsg('已复制到剪贴板')
        } catch {
          set({ modal: { kind: 'copy', text } })
        }
      },
      resetDemo: () => {
        get().confirmThen('重置演示', '将清除当前浏览器中保存的原型修改，恢复初始示例数据。', '确认重置', () => {
          try {
            localStorage.removeItem(STORE_KEY)
          } catch {
            /* ignore */
          }
          location.reload()
        })
      },
      confirmThen: (title, text, confirmLabel, fn) => set({ modal: { kind: 'confirm', title, text, confirmLabel }, modalConfirm: fn }),
    }),
    {
      name: STORE_KEY,
      partialize: (s) => ({
        libs: s.libs,
        docs: s.docs,
        credentials: s.credentials,
        members: s.members,
        sessions: s.sessions,
        modelConfig: s.modelConfig,
      }),
    },
  ),
)

/** 供组件使用的当前会话配置文本（MCP 配置示例）。 */
export function currentConfigText(client: string) {
  return configText(client)
}
