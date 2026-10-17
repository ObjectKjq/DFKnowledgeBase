import { useEffect, useRef } from 'react'

import { useKb } from '@/stores/knowledge'

import { Composer } from './Composer'
import { Home } from './Home'
import { MessageList } from './MessageList'

/** 检索会话面板（复刻原型 `.chat-panel`：空态 / 消息流 + 常驻输入框）。 */
export function ChatPage() {
  const messagesLength = useKb((s) => s.current.messages.length)
  const currentId = useKb((s) => s.current.id)
  const busy = useKb((s) => s.busy)
  const scrollRef = useRef<HTMLDivElement>(null)

  // 原型：新消息到达时把会话面板滚到底部。
  useEffect(() => {
    const el = scrollRef.current
    if (!el) return
    requestAnimationFrame(() => {
      el.scrollTop = el.scrollHeight
    })
  }, [messagesLength, busy, currentId])

  // 原型：点击输入选项弹层之外的区域时关闭弹层。
  useEffect(() => {
    const onDocClick = (e: MouseEvent) => {
      const t = e.target as HTMLElement
      if (!useKb.getState().popover) return
      if (t.closest('#popover') || t.closest('[data-menu]')) return
      useKb.getState().closePopover()
    }
    document.addEventListener('click', onDocClick)
    return () => document.removeEventListener('click', onDocClick)
  }, [])

  return (
    <section className="chat-panel" id="chat-panel" aria-label="检索会话">
      <div className="chat-scroll" id="chat-scroll" ref={scrollRef}>
        <div id="chat-content">{messagesLength ? <MessageList /> : <Home />}</div>
      </div>
      <Composer />
    </section>
  )
}
