/**
 * 全局模拟数据 store —— 复刻原型脚本中的状态与行为（defaults/current/busy/guide/preview…）。
 * 后端接口尚未实现，本 store 以前端本地数据驱动整套交互；已持久化的部分仅为领域数据。
 */
import { create } from 'zustand'
import { persist } from 'zustand/middleware'

import {
  DEFAULT_CREDENTIALS,
  DEFAULT_DOCS,
  DEFAULT_MODEL_CONFIG,
  MCP_TOOLS,
  MODES,
  STORE_KEY,
  type Credential,
  type Doc,
  type Message,
  type ModelConfig,
  type Session,
} from '@/mock/data'
import { documentUploadError } from '@/features/documents/documents'
import { getLibraries } from '@/features/knowledge/libraries'
import { configText, isTrustQuery, libName, retrieve, uuid } from '@/mock/logic'

const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms))

export interface Attachment {
  name: string
  image?: boolean
  dataUrl?: string
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
  | { kind: 'vector-confirm' }
  | { kind: 'copy'; text: string }
  | { kind: 'confirm'; title: string; text: string; confirmLabel: string }

const NEW_SESSION = (): Session => ({
  id: 'new',
  title: '新检索',
  mode: '混合检索',
  scope: [],
  skills: [],
  model: 'Qwen3 · 本地',
  tier: '均衡',
  messages: [],
})

interface KbState {
  // 领域数据
  docs: Doc[]
  credentials: Credential[]
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
  lastConfigClient: string

  // 上传
  pendingUpload: File[]
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

  createCredential: (name: string, days: number) => void
  revokeCredential: (id: string) => void

  openUpload: () => void
  addPendingFiles: (files: File[]) => void
  removePending: (index: number) => void
  setUploadTarget: (id: string) => void
  setUploadStarted: (started: boolean) => void

  saveModelConfig: (
    kind: keyof ModelConfig,
    cfg: { name: string; url: string; type: string },
  ) => void
  setLastConfigClient: (c: string) => void

  copyText: (text: string) => Promise<void>
  resetDemo: () => void
  confirmThen: (title: string, text: string, confirmLabel: string, fn: () => void) => void
}

let toastTimer: ReturnType<typeof setTimeout> | undefined

export const useKb = create<KbState>()(
  persist(
    (set, get) => ({
      docs: structuredClone(DEFAULT_DOCS),
      credentials: structuredClone(DEFAULT_CREDENTIALS),
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

      lastConfigClient: 'Codex',

      pendingUpload: [],
      uploadTarget: '',
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
        set({
          current: { ...get().current, skills: get().current.skills.filter((s) => s !== skill) },
        })
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
          ? session.scope.map((l) => libName(getLibraries(), l)).join('、')
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
          (d) =>
            d.status === '可用' &&
            getLibraries().some((library) => library.id === d.lib) &&
            (!get().current.scope.length || get().current.scope.includes(d.lib)),
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
          (d) =>
            d.status === '可用' &&
            getLibraries().some((library) => library.id === d.lib) &&
            (!get().current.scope.length || get().current.scope.includes(d.lib)),
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
          msg = {
            id: uuid(),
            type: 'answer',
            query,
            ids,
            scope,
            model: responseModel,
            web: responseWeb,
          }
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
                  options: [
                    ...getLibraries()
                      .slice(0, 2)
                      .map((library) => `优先查${library.name}`),
                    '保持当前范围',
                  ],
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
        const preferred = getLibraries().find((library) =>
          g.answers.includes(`优先查${library.name}`),
        )
        if (preferred) scope = [preferred.id]
        if (g.answers.some((a) => a.includes('图片')))
          skills = [...new Set([...skills, '图片搜索'])]
        const merged =
          g.query +
          ' ' +
          g.answers.filter((a) => !a.includes('范围') && !a.includes('优先')).join(' ')
        const scopeLabel = scope.length
          ? scope.map((l) => libName(getLibraries(), l)).join('、')
          : '全部知识库'
        set({ guide: null, current: { ...get().current, messages, scope, skills } })
        await get().executeQuery(merged, '混合检索', scopeLabel)
      },

      skipGuide: async () => {
        const { guide, current } = get()
        if (!guide) return
        if (current.messages.find((m) => m.type === 'guide' && !m.done) === undefined) return
        const messages = current.messages.map((m) =>
          m.type === 'guide' ? { ...m, done: true } : m,
        )
        const scope = current.scope.length
          ? current.scope.map((l) => libName(getLibraries(), l)).join('、')
          : '全部知识库'
        set({ guide: null, current: { ...current, messages } })
        await get().executeQuery(guide.query, '混合检索', scope)
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
          const mode = id === 'demo-rag' ? 'RAG 检索' : '混合检索'
          const sc = {
            id,
            title,
            mode,
            scope: [] as string[],
            skills: [],
            model: 'Qwen3 · 本地',
            tier: '均衡',
            messages: [] as Message[],
          }
          const clean = (demoIds[id] ?? [])
            .map((x) => get().docs.find((d) => d.id === x))
            .filter(Boolean) as Doc[]
          const msgs: Message[] = [
            { id: uuid(), type: 'user', text: title, scope: '全部知识库', mode },
            id === 'demo-rag'
              ? {
                  id: uuid(),
                  type: 'answer',
                  query: title,
                  ids: clean.map((d) => d.id),
                  scope: '全部知识库',
                  model: sc.model,
                }
              : {
                  id: uuid(),
                  type: 'results',
                  query: title,
                  ids: clean.map((d) => d.id),
                  scope: '全部知识库',
                  mode: '混合检索',
                },
          ]
          set({
            current: { ...sc, messages: msgs },
            guide: null,
            attachment: null,
            popover: '',
            previewId: null,
          })
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
        if (!getLibraries().some((l) => l.id === d.lib)) {
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
          get().toastMsg('正在上传文档，请稍候')
          return
        }
        set({ modal: null, modalConfirm: null, oneTimeToken: null, pendingUpload: [] })
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
        set({
          credentials: [...get().credentials, cred],
          oneTimeToken: token,
          modal: { kind: 'token', token },
        })
      },
      revokeCredential: (id) => {
        get().confirmThen(
          '撤销凭证',
          '撤销后，该凭证将立即无法继续调用。此操作不可恢复。',
          '确认撤销',
          () => {
            set({
              credentials: get().credentials.map((c) =>
                c.id === id ? { ...c, status: '已撤销' } : c,
              ),
            })
            get().toastMsg('凭证已撤销（演示）')
          },
        )
      },

      openUpload: () => {
        const { current } = get()
        const libs = getLibraries()
        const target =
          current.scope.length === 1 &&
          libs.some((l) => l.id === current.scope[0] && l.role !== '查看者')
            ? current.scope[0]
            : (libs.find((l) => l.role !== '查看者')?.id ?? '')
        set({
          pendingUpload: [],
          uploadTarget: target,
          uploadStarted: false,
          modal: { kind: 'upload' },
        })
      },
      addPendingFiles: (files) => {
        if (get().uploadStarted) return
        const pending = [...get().pendingUpload]
        const errors: string[] = []
        for (const file of files) {
          const error = documentUploadError(file)
          if (error) {
            errors.push(error)
            continue
          }
          if (
            !pending.some(
              (item) =>
                item.name === file.name &&
                item.size === file.size &&
                item.lastModified === file.lastModified,
            )
          )
            pending.push(file)
        }
        set({ pendingUpload: pending })
        if (errors.length) get().toastMsg(errors.join('；'), true)
      },
      removePending: (index) =>
        set({ pendingUpload: get().pendingUpload.filter((_, i) => i !== index) }),
      setUploadTarget: (id) => set({ uploadTarget: id }),
      setUploadStarted: (started) => set({ uploadStarted: started }),

      saveModelConfig: (kind, cfg) => {
        set({ modelConfig: { ...get().modelConfig, [kind]: cfg } })
        if (kind === 'chat') {
          set({
            current: {
              ...get().current,
              model: cfg.name + ' · ' + (cfg.type === '本地模型服务' ? '本地' : '私有服务'),
            },
          })
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
        get().confirmThen(
          '重置演示',
          '将清除当前浏览器中保存的原型修改，恢复初始示例数据。',
          '确认重置',
          () => {
            try {
              localStorage.removeItem(STORE_KEY)
            } catch {
              /* ignore */
            }
            location.reload()
          },
        )
      },
      confirmThen: (title, text, confirmLabel, fn) =>
        set({ modal: { kind: 'confirm', title, text, confirmLabel }, modalConfirm: fn }),
    }),
    {
      name: STORE_KEY,
      merge: (persisted, current) => {
        const saved = persisted as Partial<KbState> | undefined
        return {
          ...current,
          docs: saved?.docs ?? current.docs,
          credentials: saved?.credentials ?? current.credentials,
          modelConfig: saved?.modelConfig ?? current.modelConfig,
          sessions: (saved?.sessions ?? []).map((session) => ({
            ...session,
            mode: MODES.some(([mode]) => mode === session.mode) ? session.mode : '混合检索',
            messages: session.messages.map((message) => ({
              ...message,
              mode:
                message.mode &&
                !MODES.some(([mode]) => mode === message.mode) &&
                message.mode !== '引导补充'
                  ? '混合检索'
                  : message.mode,
            })),
          })),
        }
      },
      partialize: (s) => ({
        docs: s.docs,
        credentials: s.credentials,
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
