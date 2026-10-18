import type { ReactNode } from 'react'

/** 复刻原型 highlight()：按查询词切分并高亮命中片段。 */
function keywordsOf(q: string): string[] {
  const arr = (q.match(/[a-zA-Z0-9]+|[\u4e00-\u9fff]{2,}/g) || [])
    .flatMap((s) => (s.length > 5 ? [...s.matchAll(/.{2}/g)].map((m) => m[0]) : [s]))
    .filter((s) => s.length > 1)
  return [...new Set(arr)].sort((a, b) => b.length - a.length).slice(0, 12)
}

function escapeRe(s: string) {
  return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

/** 命中片段文本，命中词包 `<mark>`。 */
export function Highlight({ text, q }: { text: string; q: string }) {
  const keywords = keywordsOf(q)
  if (!keywords.length) return <>{text}</>
  const regex = new RegExp(keywords.map(escapeRe).join('|'), 'gi')
  const nodes: ReactNode[] = []
  let last = 0
  let key = 0
  for (const m of text.matchAll(regex)) {
    const index = m.index ?? 0
    if (index > last) nodes.push(text.slice(last, index))
    nodes.push(<mark key={key++}>{m[0]}</mark>)
    last = index + m[0].length
  }
  if (last < text.length) nodes.push(text.slice(last))
  return <>{nodes}</>
}
