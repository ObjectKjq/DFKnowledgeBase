import { stream } from '@/api/ragController'
import { assertResultSuccess } from '@/lib/http'
import type { CommonResult } from '@/lib/types'

type StreamEvent = { event: string; data: unknown }

/** 解析 SSE 行协议，处理 UTF-8 拆包、CRLF、多行 data 与终止结果。 */
export async function streamRag(
  body: API.RagAskReqVO,
  signal: AbortSignal,
  onEvent: (event: StreamEvent) => void,
) {
  // fetch adapter 返回浏览器 ReadableStream，生成器的 SseEmitter 类型仅描述后端对象。
  const response = await stream(body, {
    adapter: 'fetch',
    responseType: 'stream',
    timeout: 90000,
    signal,
    silent: true,
    headers: { Accept: 'text/event-stream' },
  })
  const reader = (response as unknown as ReadableStream<Uint8Array>).getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  let event = ''
  let data: string[] = []
  let finished = false
  const line = (value: string) => {
    if (!value) {
      if (!data.length) {
        event = ''
        return
      }
      const packet = JSON.parse(data.join('\n')) as CommonResult
      if (typeof packet.code !== 'number') throw new Error('问答流返回了无效响应')
      assertResultSuccess(packet, true)
      onEvent({ event, data: packet.data })
      if (event === 'done') finished = true
      if (event === 'error') throw new Error(packet.msg || '问答生成失败，请重试')
      event = ''
      data = []
      return
    }
    if (value.startsWith(':')) return
    const separator = value.indexOf(':')
    const name = separator < 0 ? value : value.slice(0, separator)
    const field = separator < 0 ? '' : value.slice(separator + 1).replace(/^ /, '')
    if (name === 'event') event = field
    if (name === 'data') data.push(field)
  }
  try {
    while (!finished) {
      const { value, done } = await reader.read()
      buffer += decoder.decode(value, { stream: !done })
      let index: number
      while ((index = buffer.search(/[\r\n]/)) >= 0) {
        if (!done && buffer[index] === '\r' && index === buffer.length - 1) break
        const width = buffer[index] === '\r' && buffer[index + 1] === '\n' ? 2 : 1
        line(buffer.slice(0, index))
        buffer = buffer.slice(index + width)
        if (finished) break
      }
      if (done) {
        if (buffer) line(buffer)
        if (!finished) line('')
        break
      }
    }
    if (!finished) throw new Error('问答连接已中断，未收到完整答案，请重试')
  } finally {
    await reader.cancel().catch(() => undefined)
    reader.releaseLock()
  }
}
