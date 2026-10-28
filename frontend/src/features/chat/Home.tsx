import { useQuery } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'

import { getDocPage } from '@/api/docController'
import { Icon } from '@/components/Icon'
import { useLibraries } from '@/features/knowledge/useLibraries'
import { libName } from '@/lib/format'
import { useAuthStore } from '@/stores/auth'
import { useChat } from '@/stores/chat'
import { useKb } from '@/stores/knowledge'

/** 首页以检索输入为中心，下方提供轻量操作与最近可用文档。 */
export function Home({ composer }: { composer?: ReactNode }) {
  const navigate = useNavigate()
  const token = useAuthStore((s) => s.token)
  const { libs } = useLibraries()
  const openUpload = useKb((s) => s.openUpload)
  const setMode = useChat((s) => s.setMode)
  const query = useQuery({
    queryKey: ['manage-documents', token, 'recent'],
    queryFn: async () =>
      (await getDocPage({ reqVO: { pageNum: 1, pageSize: 3, status: 4 } })).data?.list ?? [],
    enabled: !!token,
  })
  return (
    <div className="home">
      <div className="hero">
        <h1>今天想探索什么？</h1>
        <p className="hero-description">搜索资料、梳理思路，从你的知识库开始。</p>
      </div>
      {composer}
      <div className="home-actions">
        <button className="home-action" onClick={openUpload}>
          <Icon name="upload" />
          上传文档
        </button>
        <button
          className="home-action"
          onClick={() => {
            setMode('RAG 检索')
            document.getElementById('query')?.focus({ preventScroll: true })
          }}
        >
          <Icon name="spark" />
          向知识提问
        </button>
        <button className="home-action" onClick={() => navigate('/settings/mcp')}>
          <Icon name="plug" />
          连接 AI 智能体
        </button>
      </div>
      <div className="recent-docs">
        <div className="micro-heading">
          <span>最近可用文档</span>
          <button className="text-btn" onClick={() => navigate('/settings/docs')}>
            查看全部
            <Icon name="arrow" />
          </button>
        </div>
        <div className="recent-doc-list">
          {query.data?.map((doc) => (
            <button className="recent-doc" key={doc.id} onClick={() => navigate('/settings/docs')}>
              <span className="file-icon">
                <Icon name="file" />
              </span>
              <div>
                <strong>{doc.title || doc.fileName}</strong>
                <small>
                  {libName(libs, String(doc.knowledgeBaseId))} · {doc.createTime?.slice(0, 10)}
                </small>
              </div>
            </button>
          ))}
          {query.isError && (
            <button className="text-btn" onClick={() => void query.refetch()}>
              最近文档加载失败，重试
            </button>
          )}
          {query.isSuccess && !query.data.length && (
            <p className="table-note">暂无可用文档，请先上传并等待索引完成。</p>
          )}
        </div>
      </div>
    </div>
  )
}
