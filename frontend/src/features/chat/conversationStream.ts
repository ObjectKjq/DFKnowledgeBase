import request, { assertResultSuccess } from '@/lib/http'
import type { CommonResult } from '@/lib/types'

interface ConversationEvent {
  event: string
  data: unknown
}

/** 通过公共 HTTP 层发送完整选择，逐帧读取 SSE，以 done 作为成功结束标记。 */
export async function streamConversation(
  body: API.ConversationSendReqVO,
  signal: AbortSignal,
  onEvent: (event: ConversationEvent) => void,
) {
  const stream = await request<ReadableStream<Uint8Array>>('/knowledge/conversation/send-stream', {
    method: 'POST',
    data: body,
    adapter: 'fetch',
    responseType: 'stream',
    timeout: 0,
    signal,
    silent: true,
  })
  const reader = stream.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  let done = false
  const cancel = () => void reader.cancel().catch(() => undefined)
  signal.addEventListener('abort', cancel, { once: true })
  const consume = (frame: string) => {
    let event = 'message'
    const lines: string[] = []
    for (const line of frame.split(/\r?\n/)) {
      if (line.startsWith('event:')) event = line.slice(6).trim()
      if (line.startsWith('data:')) lines.push(line.slice(5).replace(/^ /, ''))
    }
    if (!lines.length) return
    const packet = JSON.parse(lines.join('\n')) as CommonResult<unknown>
    if (!packet || typeof packet.code !== 'number') throw new Error('会话流响应格式错误')
    assertResultSuccess(packet, true)
    if (event === 'error') throw new Error(packet.msg || '会话发送失败，请重试')
    onEvent({ event, data: packet.data })
    if (event === 'done') done = true
  }
  try {
    while (!done) {
      signal.throwIfAborted()
      const chunk = await reader.read()
      signal.throwIfAborted()
      buffer += decoder.decode(chunk.value, { stream: !chunk.done })
      let separator: RegExpExecArray | null
      while (!done && (separator = /\r?\n\r?\n/.exec(buffer))) {
        const frame = buffer.slice(0, separator.index)
        buffer = buffer.slice(separator.index + separator[0].length)
        consume(frame)
      }
      if (chunk.done) {
        if (!done && buffer.trim()) consume(buffer)
        break
      }
    }
    if (!done) throw new Error('会话流提前结束，请重试')
  } finally {
    signal.removeEventListener('abort', cancel)
    await reader.cancel().catch(() => undefined)
    reader.releaseLock()
  }
}
