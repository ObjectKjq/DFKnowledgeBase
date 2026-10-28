import { useEffect, useRef } from 'react'

import { useChat } from '@/stores/chat'
import { useConversation } from './conversations'

import { Composer } from './Composer'
import { Home } from './Home'
import { MessageList } from './MessageList'

/** 检索会话面板（复刻原型 `.chat-panel`：空态 / 消息流 + 常驻输入框）。 */
export function ChatPage() {
  const query = useConversation()
  const currentId = useChat((s) => s.conversationId)
  const pendingText = useChat((s) => s.pending?.text)
  const hasPending = useChat((s) => s.pending !== null)
  const busy = useChat((s) => s.busy)
  const messagesLength = query.data?.messages.length ?? 0
  const scrollRef = useRef<HTMLDivElement>(null)
  const isNew = currentId === null && !hasPending

  // 新检索从顶部开始阅读；会话中的新消息保持滚动到底部。
  useEffect(() => {
    const el = scrollRef.current
    if (!el) return
    requestAnimationFrame(() => {
      el.scrollTop = isNew ? 0 : el.scrollHeight
    })
  }, [messagesLength, busy, currentId, pendingText, isNew])

  return (
    <section
      className={`chat-panel ${isNew ? 'is-new' : ''}`}
      id="chat-panel"
      aria-label="检索会话"
    >
      <div className="chat-scroll" id="chat-scroll" ref={scrollRef}>
        <div id="chat-content">{isNew ? <Home composer={<Composer />} /> : <MessageList />}</div>
      </div>
      {!isNew && <Composer />}
    </section>
  )
}
